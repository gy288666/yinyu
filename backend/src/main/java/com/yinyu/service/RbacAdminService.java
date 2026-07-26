package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Admin;
import com.yinyu.entity.AdminRole;
import com.yinyu.entity.Permission;
import com.yinyu.entity.Role;
import com.yinyu.entity.RolePermission;
import com.yinyu.mapper.AdminMapper;
import com.yinyu.mapper.AdminRoleMapper;
import com.yinyu.mapper.PermissionMapper;
import com.yinyu.mapper.RoleMapper;
import com.yinyu.mapper.RolePermissionMapper;
import com.yinyu.security.AdminPermissionService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RBAC 管理：管理员 CRUD/分配角色、角色 CRUD/授权、权限树 CRUD（api.md 18.8）
 */
@Service
@RequiredArgsConstructor
public class RbacAdminService {

    /** 内置超级管理员账号：不可删除/停用 */
    private static final String BUILTIN_ADMIN = "admin";

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final AdminPermissionService adminPermissionService;

    // ---------------- 管理员 ----------------

    /** 管理员分页（18.8.1） */
    public PageResult<Map<String, Object>> adminPage(long pageNum, long pageSize,
                                                     String keyword, String status) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Admin::getUsername, keyword)
                    .or().like(Admin::getNickname, keyword));
        }
        if ("ENABLED".equals(status)) {
            wrapper.eq(Admin::getStatus, 1);
        } else if ("DISABLED".equals(status)) {
            wrapper.eq(Admin::getStatus, 0);
        }
        wrapper.orderByAsc(Admin::getId);
        Page<Admin> page = adminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Long> adminIds = page.getRecords().stream().map(Admin::getId).toList();
        Map<Long, List<Long>> adminRoleIds = adminIds.isEmpty() ? Map.of()
                : adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                        .in(AdminRole::getAdminId, adminIds))
                .stream().collect(Collectors.groupingBy(AdminRole::getAdminId,
                        Collectors.mapping(AdminRole::getRoleId, Collectors.toList())));
        Map<Long, Role> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        return PageResult.from(page, list -> list.stream().map(admin -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", admin.getId());
            vo.put("username", admin.getUsername());
            vo.put("name", admin.getNickname());
            List<Long> roleIds = adminRoleIds.getOrDefault(admin.getId(), List.of());
            vo.put("roleIds", roleIds);
            vo.put("roles", roleIds.stream().map(roleMap::get)
                    .filter(java.util.Objects::nonNull).map(Role::getCode).toList());
            vo.put("status", admin.getStatus() != null && admin.getStatus() == 1 ? "ENABLED" : "DISABLED");
            vo.put("builtin", BUILTIN_ADMIN.equals(admin.getUsername()));
            vo.put("createTime", admin.getCreateTime());
            return vo;
        }).toList());
    }

    /** 新增管理员（18.8.2） */
    @Transactional
    public Map<String, Object> adminCreate(Map<String, Object> body) {
        String username = Params.requireStr(body, "username");
        String password = Params.requireStr(body, "password");
        String name = Params.requireStr(body, "name");
        if (adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        admin.setNickname(name);
        admin.setStatus(1);
        adminMapper.insert(admin);
        bindRoles(admin.getId(), Params.longList(body, "roleIds"));
        return Map.of("id", admin.getId(), "username", admin.getUsername());
    }

    /** 修改管理员/分配角色（18.8.3）：内置 admin 不可停用 */
    @Transactional
    public void adminUpdate(Long id, Map<String, Object> body) {
        Admin admin = requireAdmin(id);
        String name = Params.str(body, "name");
        if (name != null) {
            admin.setNickname(name);
        }
        String status = Params.str(body, "status");
        if (status != null) {
            if ("DISABLED".equals(status)) {
                if (BUILTIN_ADMIN.equals(admin.getUsername())) {
                    throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "内置 admin 不可停用");
                }
                admin.setStatus(0);
            } else if ("ENABLED".equals(status)) {
                admin.setStatus(1);
            } else {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 ENABLED/DISABLED");
            }
        }
        adminMapper.updateById(admin);
        List<Long> roleIds = Params.longList(body, "roleIds");
        if (roleIds != null) {
            adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, id));
            bindRoles(id, roleIds);
        }
        adminPermissionService.evict(id);
    }

    /** 重置管理员密码（18.8.4） */
    public Map<String, Object> adminResetPassword(Long id) {
        Admin admin = requireAdmin(id);
        String newPassword = AdminUserService.randomPassword(10);
        admin.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        adminMapper.updateById(admin);
        return Map.of("password", newPassword);
    }

    /** 删除管理员（18.8.5）：内置 admin 不可删 */
    @Transactional
    public void adminDelete(Long id) {
        Admin admin = requireAdmin(id);
        if (BUILTIN_ADMIN.equals(admin.getUsername())) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "内置 admin 不可删除");
        }
        adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getAdminId, id));
        adminMapper.deleteById(id);
        adminPermissionService.evict(id);
    }

    private void bindRoles(Long adminId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> valid = roleMapper.selectBatchIds(roleIds).stream().map(Role::getId).toList();
        if (valid.size() != roleIds.stream().distinct().count()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "部分角色不存在");
        }
        for (Long roleId : roleIds.stream().distinct().toList()) {
            AdminRole rel = new AdminRole();
            rel.setAdminId(adminId);
            rel.setRoleId(roleId);
            adminRoleMapper.insert(rel);
        }
    }

    private Admin requireAdmin(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return admin;
    }

    // ---------------- 角色 ----------------

    /** 角色列表（18.8.6）：含成员数与已授权 permissionIds（前端回显） */
    public List<Map<String, Object>> roleList() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        Map<Long, Long> adminCounts = adminRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(AdminRole::getRoleId, Collectors.counting()));
        Map<Long, List<Long>> rolePermIds = rolePermissionMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(RolePermission::getRoleId,
                        Collectors.mapping(RolePermission::getPermissionId, Collectors.toList())));
        return roles.stream().map(role -> {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", role.getId());
            vo.put("code", role.getCode());
            vo.put("name", role.getName());
            vo.put("remark", role.getDescription());
            vo.put("status", role.getStatus() != null && role.getStatus() == 1 ? "ENABLED" : "DISABLED");
            vo.put("adminCount", adminCounts.getOrDefault(role.getId(), 0L));
            vo.put("permissionIds", rolePermIds.getOrDefault(role.getId(), List.of()));
            return vo;
        }).toList();
    }

    /** 新增角色（18.8.7） */
    public Map<String, Object> roleCreate(Map<String, Object> body) {
        String code = Params.requireStr(body, "code");
        String name = Params.requireStr(body, "name");
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code)) > 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "角色编码已存在");
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(Params.str(body, "remark"));
        role.setStatus(1);
        roleMapper.insert(role);
        return Map.of("id", role.getId(), "code", role.getCode(), "name", role.getName());
    }

    /** 修改角色（18.8.7） */
    public void roleUpdate(Long id, Map<String, Object> body) {
        Role role = requireRole(id);
        String code = Params.str(body, "code");
        if (code != null && !code.equals(role.getCode())) {
            if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code)) > 0) {
                throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "角色编码已存在");
            }
            role.setCode(code);
        }
        String name = Params.str(body, "name");
        if (name != null) {
            role.setName(name);
        }
        if (body.containsKey("remark")) {
            role.setDescription(Params.str(body, "remark"));
        }
        roleMapper.updateById(role);
        evictRoleMembers(id);
    }

    /** 删除角色（18.8.7）：被管理员引用返回 20005 */
    @Transactional
    public void roleDelete(Long id) {
        requireRole(id);
        if (adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getRoleId, id)) > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "该角色已分配给管理员，不可删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    /** 角色授权（18.8.8）：permissionIds 整体替换，成员缓存即时失效 */
    @Transactional
    public void roleGrant(Long id, List<Long> permissionIds) {
        requireRole(id);
        if (permissionIds == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: permissionIds 不能为空");
        }
        List<Long> distinct = permissionIds.stream().distinct().toList();
        if (!distinct.isEmpty()) {
            List<Long> valid = permissionMapper.selectBatchIds(distinct).stream()
                    .map(Permission::getId).toList();
            if (valid.size() != distinct.size()) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "部分权限不存在");
            }
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        for (Long permissionId : distinct) {
            RolePermission rel = new RolePermission();
            rel.setRoleId(id);
            rel.setPermissionId(permissionId);
            rolePermissionMapper.insert(rel);
        }
        evictRoleMembers(id);
    }

    private void evictRoleMembers(Long roleId) {
        adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                        .eq(AdminRole::getRoleId, roleId))
                .forEach(rel -> adminPermissionService.evict(rel.getAdminId()));
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return role;
    }

    // ---------------- 权限树 ----------------

    /** 权限树查询（18.8.9） */
    public List<Map<String, Object>> permissionTree() {
        List<Permission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort).orderByAsc(Permission::getId));
        Map<Long, List<Permission>> byParent = all.stream()
                .collect(Collectors.groupingBy(Permission::getParentId));
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Permission p : byParent.getOrDefault(0L, List.of())) {
            roots.add(permNode(p, byParent));
        }
        return roots;
    }

    private Map<String, Object> permNode(Permission p, Map<Long, List<Permission>> byParent) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", p.getId());
        node.put("parentId", p.getParentId());
        int type = p.getType() == null ? 1 : p.getType();
        node.put("type", type == 1 ? "MENU" : type == 2 ? "BUTTON" : "API");
        node.put("name", p.getName());
        node.put("path", p.getPath());
        node.put("icon", p.getIcon());
        node.put("perm", p.getCode());
        node.put("sort", p.getSort());
        node.put("enabled", p.getStatus() != null && p.getStatus() == 1);
        List<Map<String, Object>> children = new ArrayList<>();
        for (Permission child : byParent.getOrDefault(p.getId(), List.of())) {
            children.add(permNode(child, byParent));
        }
        node.put("children", children);
        return node;
    }

    /** 权限新增（18.8.10） */
    public Map<String, Object> permissionCreate(Map<String, Object> body) {
        String perm = Params.requireStr(body, "perm");
        if (permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getCode, perm)) > 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "权限标识已存在");
        }
        Permission p = new Permission();
        p.setName(Params.requireStr(body, "name"));
        p.setCode(perm);
        p.setType(permType(Params.str(body, "type")));
        Long parentId = Params.lng(body, "parentId");
        p.setParentId(parentId == null ? 0L : parentId);
        p.setPath(Params.str(body, "path"));
        p.setIcon(Params.str(body, "icon"));
        Integer sort = Params.integer(body, "sort");
        p.setSort(sort == null ? 0 : sort);
        p.setStatus(1);
        permissionMapper.insert(p);
        return Map.of("id", p.getId(), "perm", p.getCode());
    }

    /** 权限修改（18.8.10） */
    public void permissionUpdate(Long id, Map<String, Object> body) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String perm = Params.str(body, "perm");
        if (perm != null && !perm.equals(p.getCode())) {
            if (permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getCode, perm)) > 0) {
                throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "权限标识已存在");
            }
            p.setCode(perm);
        }
        String name = Params.str(body, "name");
        if (name != null) {
            p.setName(name);
        }
        String type = Params.str(body, "type");
        if (type != null) {
            p.setType(permType(type));
        }
        Long parentId = Params.lng(body, "parentId");
        if (parentId != null) {
            p.setParentId(parentId);
        }
        if (body.containsKey("path")) {
            p.setPath(Params.str(body, "path"));
        }
        if (body.containsKey("icon")) {
            p.setIcon(Params.str(body, "icon"));
        }
        Integer sort = Params.integer(body, "sort");
        if (sort != null) {
            p.setSort(sort);
        }
        Boolean enabled = Params.bool(body, "enabled");
        if (enabled != null) {
            p.setStatus(enabled ? 1 : 0);
        }
        permissionMapper.updateById(p);
    }

    /** 权限删除（18.8.10）：有子节点返回 20005，同时清理角色关联 */
    @Transactional
    public void permissionDelete(Long id) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getParentId, id)) > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在子权限，不可删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getPermissionId, id));
        permissionMapper.deleteById(id);
    }

    private int permType(String type) {
        if (type == null) {
            return 2;
        }
        return switch (type.toUpperCase()) {
            case "MENU" -> 1;
            case "BUTTON" -> 2;
            case "API" -> 3;
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: type 须为 MENU/BUTTON/API");
        };
    }
}
