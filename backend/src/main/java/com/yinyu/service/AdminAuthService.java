package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.exception.BizException;
import com.yinyu.dto.AdminLoginRequest;
import com.yinyu.entity.Admin;
import com.yinyu.entity.Permission;
import com.yinyu.mapper.AdminMapper;
import com.yinyu.mapper.PermissionMapper;
import com.yinyu.security.AdminPermissionService;
import com.yinyu.security.JwtUtil;
import com.yinyu.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 后台管理员认证：验证码/登录/登出/当前信息（含 RBAC 权限码与菜单）
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminMapper adminMapper;
    private final PermissionMapper permissionMapper;
    private final AdminPermissionService adminPermissionService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final StringRedisTemplate redis;

    /** 生成验证码（5 分钟有效，api.md 2.9） */
    public Map<String, Object> captcha() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        String key = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(RedisKeys.ADMIN_CAPTCHA_PREFIX + key,
                code.toString(), Duration.ofMinutes(5));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("captchaKey", key);
        data.put("imageBase64", drawCaptcha(code.toString()));
        return data;
    }

    private String drawCaptcha(String code) {
        try {
            BufferedImage img = new BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(245, 246, 250));
            g.fillRect(0, 0, 100, 40);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(RANDOM.nextInt(120), RANDOM.nextInt(120), RANDOM.nextInt(120)));
                g.drawString(String.valueOf(code.charAt(i)), 12 + i * 22, 28 + RANDOM.nextInt(6) - 3);
            }
            for (int i = 0; i < 4; i++) {
                g.drawLine(RANDOM.nextInt(100), RANDOM.nextInt(40), RANDOM.nextInt(100), RANDOM.nextInt(40));
            }
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            // 无图形环境等异常时返回空图，验证码仍存于 Redis
            return "";
        }
    }

    public Map<String, Object> login(AdminLoginRequest req) {
        // 验证码校验（一次性）
        String captchaKey = RedisKeys.ADMIN_CAPTCHA_PREFIX + req.getCaptchaKey();
        String expected = redis.opsForValue().getAndDelete(captchaKey);
        if (expected == null || !expected.equalsIgnoreCase(req.getCaptchaCode())) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR, "验证码错误或过期");
        }
        String lockKey = RedisKeys.LOGIN_LOCK_PREFIX + "ADMIN:" + req.getUsername();
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED, "账号已锁定，请10分钟后再试");
        }
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, req.getUsername()));
        if (admin == null || !BCrypt.checkpw(req.getPassword(), admin.getPassword())) {
            String failKey = RedisKeys.LOGIN_FAIL_PREFIX + "ADMIN:" + req.getUsername();
            Long count = redis.opsForValue().increment(failKey);
            redis.expire(failKey, LOCK_DURATION);
            if (count != null && count >= MAX_FAIL) {
                redis.opsForValue().set(lockKey, "1", LOCK_DURATION);
                redis.delete(failKey);
            }
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
        }
        redis.delete(RedisKeys.LOGIN_FAIL_PREFIX + "ADMIN:" + req.getUsername());
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        Map<String, Object> data = profile(admin);
        data.put("accessToken", jwtUtil.createAccessToken(JwtUtil.TYPE_ADMIN, admin.getId()));
        data.put("refreshToken", jwtUtil.createRefreshToken(JwtUtil.TYPE_ADMIN, admin.getId()));
        data.put("expiresIn", jwtUtil.getAccessExpireSeconds());
        return data;
    }

    public void logout(String token) {
        if (token != null) {
            blacklistService.blacklist(token, jwtUtil.getAccessExpireSeconds());
        }
    }

    /** 当前管理员信息（api.md 2.12），角色/权限码/菜单 */
    public Map<String, Object> me(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
        }
        return profile(admin);
    }

    private Map<String, Object> profile(Admin admin) {
        Set<String> roles = adminPermissionService.getRoleCodes(admin.getId());
        boolean superAdmin = roles.contains(AdminPermissionService.SUPER_ADMIN);
        Set<String> perms = superAdmin
                ? allPermissionCodes()
                : adminPermissionService.getPermissionCodes(admin.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adminId", admin.getId());
        data.put("name", admin.getNickname());
        data.put("roles", roles);
        data.put("permissions", perms);
        data.put("menus", buildMenus(superAdmin, perms));
        return data;
    }

    private Set<String> allPermissionCodes() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>().eq(Permission::getStatus, 1))
                .stream().map(Permission::getCode).collect(java.util.stream.Collectors.toSet());
    }

    /** 动态菜单：菜单型权限（type=1）按持有权限过滤，两级树 */
    private List<Map<String, Object>> buildMenus(boolean superAdmin, Set<String> perms) {
        List<Permission> menus = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getType, 1).eq(Permission::getStatus, 1)
                        .orderByAsc(Permission::getSort));
        List<Permission> owned = menus.stream()
                .filter(p -> superAdmin || perms.contains(p.getCode())).toList();
        Set<Long> ownedIds = owned.stream().map(Permission::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Permission p : owned.stream().filter(p -> p.getParentId() == 0)
                .sorted(Comparator.comparing(Permission::getSort)).toList()) {
            Map<String, Object> node = menuNode(p);
            List<Map<String, Object>> children = owned.stream()
                    .filter(c -> c.getParentId().equals(p.getId()) && ownedIds.contains(c.getId()))
                    .sorted(Comparator.comparing(Permission::getSort))
                    .map(this::menuNode).toList();
            node.put("children", children);
            roots.add(node);
        }
        return roots;
    }

    private Map<String, Object> menuNode(Permission p) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", p.getId());
        node.put("name", p.getName());
        node.put("path", p.getPath());
        node.put("icon", p.getIcon());
        node.put("perm", p.getCode());
        return node;
    }
}
