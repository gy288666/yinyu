package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 下载（api.md 14.x）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    /** 获取下载地址（api.md 14.1） */
    @GetMapping("/songs/{id}/download-url")
    @RequireUser
    public Result<Map<String, Object>> downloadUrl(@PathVariable Long id) {
        return Result.success(downloadService.downloadUrl(id, AuthContext.userId()));
    }

    /** 我的下载记录（api.md 14.2） */
    @GetMapping("/downloads")
    @RequireUser
    public Result<PageResult<Map<String, Object>>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(downloadService.records(AuthContext.userId(), pageNum, pageSize));
    }
}
