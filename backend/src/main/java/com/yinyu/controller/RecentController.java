package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.RecentPlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 最近播放（api.md 12.1/12.2）；写入由 GET /api/songs/{id}/url 服务端自动完成
 */
@RestController
@RequestMapping("/api/recent")
@RequiredArgsConstructor
@RequireUser
public class RecentController {

    private final RecentPlayService recentPlayService;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(recentPlayService.page(AuthContext.userId(), pageNum, pageSize));
    }

    @DeleteMapping
    public Result<Void> clear() {
        recentPlayService.clear(AuthContext.userId());
        return Result.success();
    }
}
