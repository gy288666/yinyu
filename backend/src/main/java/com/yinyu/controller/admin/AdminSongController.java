package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.dto.AdminSongRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminSongService;
import com.yinyu.vo.AdminSongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 后台音乐管理（api.md 18.2）
 */
@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
public class AdminSongController {

    private final AdminSongService adminSongService;

    @GetMapping
    @RequireAdmin(permission = "music:list")
    public Result<PageResult<AdminSongVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Long singerId,
                                                @RequestParam(required = false) Long albumId,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String status) {
        return Result.success(adminSongService.page(pageNum, pageSize, name, singerId, albumId, categoryId, status));
    }

    /** 上传音频文件（MinIO music 桶，100MB 限制见 multipart 配置） */
    @PostMapping("/upload")
    @RequireAdmin(permission = "music:add")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(adminSongService.upload(file));
    }

    @PostMapping
    @RequireAdmin(permission = "music:add")
    public Result<AdminSongVO> create(@RequestBody AdminSongRequest req) {
        return Result.success(adminSongService.create(req, AuthContext.adminId()));
    }

    @PutMapping("/{id}")
    @RequireAdmin(permission = "music:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody AdminSongRequest req) {
        adminSongService.update(id, req);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireAdmin(permission = "music:shelf")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminSongService.changeStatus(id, body.get("status"));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin(permission = "music:delete")
    public Result<Void> delete(@PathVariable Long id) {
        adminSongService.delete(id);
        return Result.success();
    }
}
