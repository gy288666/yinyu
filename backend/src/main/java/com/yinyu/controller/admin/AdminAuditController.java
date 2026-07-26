package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminSongService;
import com.yinyu.vo.AdminSongVO;
import com.yinyu.vo.PlayUrlVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台音乐审核（api.md 18.3）
 */
@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminSongService adminSongService;

    @GetMapping
    @RequireAdmin(permission = "music:audit")
    public Result<PageResult<AdminSongVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(defaultValue = "PENDING") String status) {
        return Result.success(adminSongService.auditPage(pageNum, pageSize, status));
    }

    @PutMapping("/{songId}/pass")
    @RequireAdmin(permission = "music:audit:pass")
    public Result<Void> pass(@PathVariable Long songId) {
        adminSongService.pass(songId, AuthContext.adminId());
        return Result.success();
    }

    @PutMapping("/{songId}/reject")
    @RequireAdmin(permission = "music:audit:reject")
    public Result<Void> reject(@PathVariable Long songId, @RequestBody Map<String, String> body) {
        adminSongService.reject(songId, body.get("reason"), AuthContext.adminId());
        return Result.success();
    }

    /** 审核试听：不要求上架、不埋点 */
    @GetMapping("/{songId}/url")
    @RequireAdmin(permission = "music:audit")
    public Result<PlayUrlVO> auditUrl(@PathVariable Long songId) {
        return Result.success(adminSongService.auditUrl(songId));
    }
}
