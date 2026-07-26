package com.yinyu.controller;

import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.service.RadioService;
import com.yinyu.vo.SongVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 电台（api.md 10.6/10.7 + 详情/节目列表补充）
 */
@RestController
@RequestMapping("/api/radios")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(radioService.list());
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(radioService.detail(id));
    }

    @GetMapping("/{id}/programs")
    public Result<List<Map<String, Object>>> programs(@PathVariable Long id) {
        return Result.success(radioService.programs(id));
    }

    /** 随机下一曲：一轮内不重复 */
    @GetMapping("/{id}/next")
    public Result<SongVO> next(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(radioService.next(id, AuthContext.userId(), request.getRemoteAddr()));
    }
}
