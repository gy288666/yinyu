package com.yinyu.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.entity.AdminRole;
import com.yinyu.entity.Permission;
import com.yinyu.entity.Role;
import com.yinyu.entity.RolePermission;
import com.yinyu.mapper.AdminRoleMapper;
import com.yinyu.mapper.PermissionMapper;
import com.yinyu.mapper.RoleMapper;
import com.yinyu.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员 RBAC：查 admin_role/role_permission/permission，结果缓存 Redis 30 分钟
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final AdminRoleMapper adminRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final StringRedisTemplate redis;

    /** 管理员角色编码集合（缓存） */
    public Set<String> getRoleCodes(Long adminId) {
        String cacheKey = RedisKeys.ADMIN_ROLES_PREFIX + adminId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? Collections.emptySet() : Set.of(cached.split(","));
        }
        List<Long> roleIds = adminRoleMapper.selectList(
                        new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, adminId))
                .stream().map(AdminRole::getRoleId).toList();
        Set<String> codes = roleIds.isEmpty() ? Collections.emptySet()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                        .map(Role::getCode).collect(Collectors.toSet());
        redis.opsForValue().set(cacheKey, String.join(",", codes), CACHE_TTL);
        return codes;
    }

    /** 管理员权限编码集合（缓存） */
    public Set<String> getPermissionCodes(Long adminId) {
        String cacheKey = RedisKeys.ADMIN_PERMS_PREFIX + adminId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? Collections.emptySet() : Set.of(cached.split(","));
        }
        List<Long> roleIds = adminRoleMapper.selectList(
                        new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, adminId))
                .stream().map(AdminRole::getRoleId).toList();
        Set<String> codes = Collections.emptySet();
        if (!roleIds.isEmpty()) {
            List<Long> permIds = rolePermissionMapper.selectList(
                            new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                    .stream().map(RolePermission::getPermissionId).distinct().toList();
            if (!permIds.isEmpty()) {
                codes = permissionMapper.selectBatchIds(permIds).stream()
                        .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                        .map(Permission::getCode).collect(Collectors.toSet());
            }
        }
        redis.opsForValue().set(cacheKey, String.join(",", codes), CACHE_TTL);
        return codes;
    }

    /** 权限判定：SUPER_ADMIN 角色放行全部 */
    public boolean hasPermission(Long adminId, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        if (getRoleCodes(adminId).contains(SUPER_ADMIN)) {
            return true;
        }
        return getPermissionCodes(adminId).contains(permission);
    }

    /** 角色/权限变更后清缓存 */
    public void evict(Long adminId) {
        redis.delete(RedisKeys.ADMIN_ROLES_PREFIX + adminId);
        redis.delete(RedisKeys.ADMIN_PERMS_PREFIX + adminId);
    }
}
