package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.RbacAdminService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RBAC 管理（api.md 18.8）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRbacController {

    private final RbacAdminService rbacAdminService;

    // ---------------- 管理员 ----------------

    @GetMapping("/admins")
    @RequireAdmin(permission = "system:admin:list")
    public Result<PageResult<Map<String, Object>>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                                             @RequestParam(defaultValue = "10") long pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status) {
        return Result.success(rbacAdminService.adminPage(pageNum, pageSize, keyword, status));
    }

    @PostMapping("/admins")
    @RequireAdmin(permission = "system:admin:add")
    public Result<Map<String, Object>> adminCreate(@RequestBody Map<String, Object> body) {
        return Result.success(rbacAdminService.adminCreate(body));
    }

    @PutMapping("/admins/{id}")
    @RequireAdmin(permission = "system:admin:edit")
    public Result<Void> adminUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        rbacAdminService.adminUpdate(id, body);
        return Result.success();
    }

    @PutMapping("/admins/{id}/password/reset")
    @RequireAdmin(permission = "system:admin:reset")
    public Result<Map<String, Object>> adminResetPassword(@PathVariable Long id) {
        return Result.success(rbacAdminService.adminResetPassword(id));
    }

    @DeleteMapping("/admins/{id}")
    @RequireAdmin(permission = "system:admin:delete")
    public Result<Void> adminDelete(@PathVariable Long id) {
        rbacAdminService.adminDelete(id);
        return Result.success();
    }

    // ---------------- 角色 ----------------

    @GetMapping("/roles")
    @RequireAdmin(permission = "system:role:list")
    public Result<List<Map<String, Object>>> roleList() {
        return Result.success(rbacAdminService.roleList());
    }

    @PostMapping("/roles")
    @RequireAdmin(permission = "system:role:add")
    public Result<Map<String, Object>> roleCreate(@RequestBody Map<String, Object> body) {
        return Result.success(rbacAdminService.roleCreate(body));
    }

    @PutMapping("/roles/{id}")
    @RequireAdmin(permission = "system:role:edit")
    public Result<Void> roleUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        rbacAdminService.roleUpdate(id, body);
        return Result.success();
    }

    @DeleteMapping("/roles/{id}")
    @RequireAdmin(permission = "system:role:delete")
    public Result<Void> roleDelete(@PathVariable Long id) {
        rbacAdminService.roleDelete(id);
        return Result.success();
    }

    /** 角色授权：permissionIds 整体替换 */
    @PutMapping("/roles/{id}/permissions")
    @RequireAdmin(permission = "system:role:grant")
    public Result<Void> roleGrant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        rbacAdminService.roleGrant(id, Params.longList(body, "permissionIds"));
        return Result.success();
    }

    // ---------------- 权限树 ----------------

    @GetMapping("/permissions")
    @RequireAdmin(permission = "system:perm:list")
    public Result<List<Map<String, Object>>> permissionTree() {
        return Result.success(rbacAdminService.permissionTree());
    }

    @PostMapping("/permissions")
    @RequireAdmin(permission = "system:perm:edit")
    public Result<Map<String, Object>> permissionCreate(@RequestBody Map<String, Object> body) {
        return Result.success(rbacAdminService.permissionCreate(body));
    }

    @PutMapping("/permissions/{id}")
    @RequireAdmin(permission = "system:perm:edit")
    public Result<Void> permissionUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        rbacAdminService.permissionUpdate(id, body);
        return Result.success();
    }

    @DeleteMapping("/permissions/{id}")
    @RequireAdmin(permission = "system:perm:edit")
    public Result<Void> permissionDelete(@PathVariable Long id) {
        rbacAdminService.permissionDelete(id);
        return Result.success();
    }
}
