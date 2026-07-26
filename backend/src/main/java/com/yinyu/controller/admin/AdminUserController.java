package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.entity.UserLevel;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminUserService;
import com.yinyu.service.FeedbackService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台用户运营：用户/会员/等级/反馈（api.md 18.5）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final FeedbackService feedbackService;

    // ---------------- 用户管理 ----------------

    @GetMapping("/users")
    @RequireAdmin(permission = "user:list")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Boolean vip) {
        return Result.success(adminUserService.page(pageNum, pageSize, keyword, status, vip));
    }

    @GetMapping("/users/{id}")
    @RequireAdmin(permission = "user:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(adminUserService.detail(id));
    }

    @PutMapping("/users/{id}/status")
    @RequireAdmin(permission = "user:disable")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.changeStatus(id, Params.requireStr(body, "status"));
        return Result.success();
    }

    @PutMapping("/users/{id}/password/reset")
    @RequireAdmin(permission = "user:reset")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id) {
        return Result.success(adminUserService.resetPassword(id));
    }

    // ---------------- 会员管理 ----------------

    @GetMapping("/vips")
    @RequireAdmin(permission = "vip:list")
    public Result<PageResult<Map<String, Object>>> vipPage(@RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "10") long pageSize,
                                                           @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.vipPage(pageNum, pageSize, keyword));
    }

    @PutMapping("/vips/{userId}")
    @RequireAdmin(permission = "vip:adjust")
    public Result<Map<String, Object>> adjustVip(@PathVariable Long userId,
                                                 @RequestBody Map<String, Object> body) {
        return Result.success(adminUserService.adjustVip(userId, Params.integer(body, "deltaDays")));
    }

    // ---------------- 等级规则 ----------------

    @GetMapping("/levels")
    @RequireAdmin(permission = "level:list")
    public Result<List<UserLevel>> levels() {
        return Result.success(adminUserService.levels());
    }

    @PostMapping("/levels")
    @RequireAdmin(permission = "level:edit")
    public Result<UserLevel> levelCreate(@RequestBody Map<String, Object> body) {
        return Result.success(adminUserService.levelCreate(body));
    }

    @PutMapping("/levels/{id}")
    @RequireAdmin(permission = "level:edit")
    public Result<Void> levelUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.levelUpdate(id, body);
        return Result.success();
    }

    @DeleteMapping("/levels/{id}")
    @RequireAdmin(permission = "level:edit")
    public Result<Void> levelDelete(@PathVariable Long id) {
        adminUserService.levelDelete(id);
        return Result.success();
    }

    // ---------------- 反馈处理 ----------------

    @GetMapping("/feedbacks")
    @RequireAdmin(permission = "feedback:list")
    public Result<PageResult<Map<String, Object>>> feedbackPage(@RequestParam(defaultValue = "1") long pageNum,
                                                                @RequestParam(defaultValue = "10") long pageSize,
                                                                @RequestParam(required = false) String type,
                                                                @RequestParam(required = false) String status) {
        return Result.success(feedbackService.adminPage(pageNum, pageSize, type, status));
    }

    @PutMapping("/feedbacks/{id}")
    @RequireAdmin(permission = "feedback:handle")
    public Result<Void> handleFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        feedbackService.handle(id, body, AuthContext.adminId());
        return Result.success();
    }
}
