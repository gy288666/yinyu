package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.service.OpsContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营内容门户展示：轮播图/公告/活动（api.md 16.x）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OpsController {

    private final OpsContentService opsContentService;

    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> banners() {
        return Result.success(opsContentService.banners());
    }

    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(opsContentService.notices(limit));
    }

    @GetMapping("/notices/{id}")
    public Result<Map<String, Object>> noticeDetail(@PathVariable Long id) {
        return Result.success(opsContentService.noticeDetail(id));
    }

    @GetMapping("/activities")
    public Result<PageResult<Map<String, Object>>> activities(@RequestParam(defaultValue = "1") long pageNum,
                                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(opsContentService.activities(pageNum, pageSize));
    }

    @GetMapping("/activities/{id}")
    public Result<Map<String, Object>> activityDetail(@PathVariable Long id) {
        return Result.success(opsContentService.activityDetail(id));
    }
}
