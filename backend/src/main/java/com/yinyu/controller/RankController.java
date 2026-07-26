package com.yinyu.controller;

import com.yinyu.common.result.Result;
import com.yinyu.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排行榜（api.md 9.1/9.2）
 */
@RestController
@RequestMapping("/api/ranks")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping
    public Result<List<Map<String, Object>>> overview() {
        return Result.success(rankService.overview());
    }

    @GetMapping("/{type}")
    public Result<Map<String, Object>> detail(@PathVariable String type,
                                              @RequestParam(defaultValue = "50") int limit) {
        return Result.success(rankService.detail(type.toUpperCase(), limit, false));
    }
}
