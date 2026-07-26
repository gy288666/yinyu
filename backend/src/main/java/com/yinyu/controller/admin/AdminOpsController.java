package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.OpsContentService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 后台运营内容管理：轮播图/公告/活动 CRUD + 通用图片上传（api.md 18.6）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOpsController {

    private final OpsContentService opsContentService;

    // ---------------- 轮播图 ----------------

    @GetMapping("/banners")
    @RequireAdmin(permission = "banner:list")
    public Result<PageResult<Map<String, Object>>> bannerPage(@RequestParam(defaultValue = "1") long pageNum,
                                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(opsContentService.bannerPage(pageNum, pageSize));
    }

    @PostMapping("/banners")
    @RequireAdmin(permission = "banner:add")
    public Result<Map<String, Object>> bannerCreate(@RequestBody Map<String, Object> body) {
        return Result.success(opsContentService.bannerCreate(body));
    }

    @PutMapping("/banners/{id}")
    @RequireAdmin(permission = "banner:edit")
    public Result<Void> bannerUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        opsContentService.bannerUpdate(id, body);
        return Result.success();
    }

    @DeleteMapping("/banners/{id}")
    @RequireAdmin(permission = "banner:delete")
    public Result<Void> bannerDelete(@PathVariable Long id) {
        opsContentService.bannerDelete(id);
        return Result.success();
    }

    // ---------------- 公告 ----------------

    @GetMapping("/notices")
    @RequireAdmin(permission = "notice:list")
    public Result<PageResult<Map<String, Object>>> noticePage(@RequestParam(defaultValue = "1") long pageNum,
                                                              @RequestParam(defaultValue = "10") long pageSize,
                                                              @RequestParam(required = false) String title) {
        return Result.success(opsContentService.noticePage(pageNum, pageSize, title));
    }

    @PostMapping("/notices")
    @RequireAdmin(permission = "notice:add")
    public Result<Map<String, Object>> noticeCreate(@RequestBody Map<String, Object> body) {
        return Result.success(opsContentService.noticeCreate(body, AuthContext.adminId()));
    }

    @PutMapping("/notices/{id}")
    @RequireAdmin(permission = "notice:edit")
    public Result<Void> noticeUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        opsContentService.noticeUpdate(id, body);
        return Result.success();
    }

    @DeleteMapping("/notices/{id}")
    @RequireAdmin(permission = "notice:delete")
    public Result<Void> noticeDelete(@PathVariable Long id) {
        opsContentService.noticeDelete(id);
        return Result.success();
    }

    // ---------------- 活动 ----------------

    @GetMapping("/activities")
    @RequireAdmin(permission = "activity:list")
    public Result<PageResult<Map<String, Object>>> activityPage(@RequestParam(defaultValue = "1") long pageNum,
                                                                @RequestParam(defaultValue = "10") long pageSize,
                                                                @RequestParam(required = false) String title) {
        return Result.success(opsContentService.activityPage(pageNum, pageSize, title));
    }

    @PostMapping("/activities")
    @RequireAdmin(permission = "activity:add")
    public Result<Map<String, Object>> activityCreate(@RequestBody Map<String, Object> body) {
        return Result.success(opsContentService.activityCreate(body));
    }

    @PutMapping("/activities/{id}")
    @RequireAdmin(permission = "activity:edit")
    public Result<Void> activityUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        opsContentService.activityUpdate(id, body);
        return Result.success();
    }

    @PutMapping("/activities/{id}/status")
    @RequireAdmin(permission = "activity:shelf")
    public Result<Void> activityStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        opsContentService.activityStatus(id, Params.requireStr(body, "status"));
        return Result.success();
    }

    @DeleteMapping("/activities/{id}")
    @RequireAdmin(permission = "activity:delete")
    public Result<Void> activityDelete(@PathVariable Long id) {
        opsContentService.activityDelete(id);
        return Result.success();
    }

    // ---------------- 通用图片上传（18.6.14） ----------------

    @PostMapping("/upload/image")
    @RequireAdmin
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(opsContentService.uploadImage(file));
    }
}
