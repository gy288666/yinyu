package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.User;
import com.yinyu.entity.UserLevel;
import com.yinyu.entity.UserLikeSong;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.UserLevelMapper;
import com.yinyu.mapper.UserLikeSongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.security.UserStatusChecker;
import com.yinyu.util.MinioUtil;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台用户运营：用户管理 / 会员管理 / 等级规则（api.md 18.5）
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final UserLevelMapper userLevelMapper;
    private final UserLikeSongMapper userLikeSongMapper;
    private final PlaylistMapper playlistMapper;
    private final UserStatusChecker userStatusChecker;
    private final MinioUtil minioUtil;

    // ---------------- 用户管理 ----------------

    /** 用户分页（18.5.1）：keyword 匹配用户名/昵称，status/vip 筛选 */
    public PageResult<Map<String, Object>> page(long pageNum, long pageSize, String keyword,
                                                String status, Boolean vip) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        if ("ENABLED".equals(status)) {
            wrapper.eq(User::getStatus, 1);
        } else if ("DISABLED".equals(status)) {
            wrapper.eq(User::getStatus, 0);
        } else if (status != null && !status.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 ENABLED/DISABLED");
        }
        if (Boolean.TRUE.equals(vip)) {
            wrapper.eq(User::getIsVip, 1).gt(User::getVipExpireTime, LocalDateTime.now());
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, list -> list.stream().map(this::toVO).toList());
    }

    /** 用户详情（18.5.2） */
    public Map<String, Object> detail(Long id) {
        User user = requireUser(id);
        Map<String, Object> vo = toVO(user);
        vo.put("gender", user.getGender());
        vo.put("email", user.getEmail());
        vo.put("phone", user.getPhone());
        vo.put("signature", user.getSignature());
        vo.put("exp", user.getExp());
        vo.put("likeCount", userLikeSongMapper.selectCount(
                new LambdaQueryWrapper<UserLikeSong>().eq(UserLikeSong::getUserId, id)));
        vo.put("playlistCount", playlistMapper.selectCount(
                new LambdaQueryWrapper<Playlist>().eq(Playlist::getUserId, id)));
        return vo;
    }

    /** 启用/停用（18.5.3）：停用写 Redis 标记使已签发 token 即时失效 */
    public void changeStatus(Long id, String status) {
        User user = requireUser(id);
        if ("ENABLED".equals(status)) {
            user.setStatus(1);
            userStatusChecker.markEnabled(id);
        } else if ("DISABLED".equals(status)) {
            user.setStatus(0);
            userStatusChecker.markDisabled(id);
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 ENABLED/DISABLED");
        }
        userMapper.updateById(user);
    }

    /** 重置密码（18.5.4）：返回随机密码 */
    public Map<String, Object> resetPassword(Long id) {
        User user = requireUser(id);
        String newPassword = randomPassword(10);
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.updateById(user);
        return Map.of("password", newPassword);
    }

    public static String randomPassword(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return user;
    }

    private Map<String, Object> toVO(User user) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname());
        vo.put("avatar", minioUtil.publicImageUrl(user.getAvatar()));
        vo.put("level", user.getLevelId());
        vo.put("vip", AuthService.isVipActive(user));
        vo.put("vipExpireAt", user.getVipExpireTime());
        vo.put("status", user.getStatus() != null && user.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.put("channel", user.getRegisterChannel());
        vo.put("lastLoginAt", user.getLastLoginTime());
        vo.put("createTime", user.getCreateTime());
        return vo;
    }

    // ---------------- 会员管理 ----------------

    /** VIP 用户分页（18.5.5） */
    public PageResult<Map<String, Object>> vipPage(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getIsVip, 1)
                .gt(User::getVipExpireTime, LocalDateTime.now())
                .orderByAsc(User::getVipExpireTime);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, list -> list.stream().map(this::toVO).toList());
    }

    /** 调整 VIP 时长（18.5.6）：deltaDays 可正可负 */
    public Map<String, Object> adjustVip(Long userId, Integer deltaDays) {
        if (deltaDays == null || deltaDays == 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: deltaDays 不能为空或 0");
        }
        User user = requireUser(userId);
        LocalDateTime base = user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now())
                ? user.getVipExpireTime() : LocalDateTime.now();
        LocalDateTime newExpire = base.plusDays(deltaDays);
        user.setVipExpireTime(newExpire);
        user.setIsVip(newExpire.isAfter(LocalDateTime.now()) ? 1 : 0);
        userMapper.updateById(user);
        return Map.of("userId", userId, "vipExpireAt",
                newExpire.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    // ---------------- 等级规则 ----------------

    /** 等级列表（18.5.7） */
    public List<UserLevel> levels() {
        return userLevelMapper.selectList(
                new LambdaQueryWrapper<UserLevel>().orderByAsc(UserLevel::getLevel));
    }

    public UserLevel levelCreate(Map<String, Object> body) {
        Integer level = Params.integer(body, "level");
        String name = Params.requireStr(body, "name");
        if (level == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: level 不能为空");
        }
        if (userLevelMapper.selectCount(new LambdaQueryWrapper<UserLevel>()
                .eq(UserLevel::getLevel, level)) > 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "该等级已存在");
        }
        UserLevel entity = new UserLevel();
        entity.setLevel(level);
        entity.setName(name);
        Long minExp = Params.lng(body, "minExp");
        entity.setMinExp(minExp == null ? 0 : minExp);
        entity.setIcon(Params.str(body, "icon"));
        entity.setPrivilege(Params.str(body, "privilege"));
        userLevelMapper.insert(entity);
        return entity;
    }

    public void levelUpdate(Long id, Map<String, Object> body) {
        UserLevel entity = userLevelMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String name = Params.str(body, "name");
        if (name != null) {
            entity.setName(name);
        }
        Long minExp = Params.lng(body, "minExp");
        if (minExp != null) {
            entity.setMinExp(minExp);
        }
        if (body.containsKey("icon")) {
            entity.setIcon(Params.str(body, "icon"));
        }
        if (body.containsKey("privilege")) {
            entity.setPrivilege(Params.str(body, "privilege"));
        }
        userLevelMapper.updateById(entity);
    }

    public void levelDelete(Long id) {
        UserLevel entity = userLevelMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getLevelId, id)) > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在用户引用该等级，不可删除");
        }
        userLevelMapper.deleteById(id);
    }
}
