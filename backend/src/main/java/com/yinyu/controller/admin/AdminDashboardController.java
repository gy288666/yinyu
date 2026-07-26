package com.yinyu.controller.admin;

import com.yinyu.common.result.Result;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台数据看板（api.md 18.1.1-18.1.5）
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@RequireAdmin(permission = "dashboard:view")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.success(dashboardService.summary());
    }

    @GetMapping("/play-trend")
    public Result<List<Map<String, Object>>> playTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.playTrend(days));
    }

    @GetMapping("/user-channels")
    public Result<List<Map<String, Object>>> userChannels() {
        return Result.success(dashboardService.userChannels());
    }

    @GetMapping("/hot-songs")
    public Result<List<Map<String, Object>>> hotSongs() {
        return Result.success(dashboardService.hotSongs());
    }

    /** 实时动态（18.1.4） */
    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(dashboardService.events(limit));
    }
}
