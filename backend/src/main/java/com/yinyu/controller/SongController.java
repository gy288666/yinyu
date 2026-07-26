package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.service.SongService;
import com.yinyu.vo.PlayUrlVO;
import com.yinyu.vo.SongDetailVO;
import com.yinyu.vo.SongVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门户歌曲接口（api.md 3.1-3.5）
 */
@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping
    public Result<PageResult<SongVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long singerId,
                                           @RequestParam(required = false) Boolean vip,
                                           @RequestParam(required = false) String quality,
                                           @RequestParam(defaultValue = "latest") String sort) {
        return Result.success(songService.page(pageNum, pageSize, categoryId, singerId, vip, quality, sort));
    }

    @GetMapping("/newest")
    public Result<List<SongVO>> newest(@RequestParam(defaultValue = "12") int limit) {
        return Result.success(songService.newest(limit));
    }

    @GetMapping("/{id}")
    public Result<SongDetailVO> detail(@PathVariable Long id) {
        return Result.success(songService.detail(id, AuthContext.userId()));
    }

    @GetMapping("/{id}/lyric")
    public Result<Object> lyric(@PathVariable Long id) {
        return Result.success(songService.lyric(id));
    }

    /** 核心：播放地址 + Redis 埋点 + 最近播放记录 */
    @GetMapping("/{id}/url")
    public Result<PlayUrlVO> playUrl(@PathVariable Long id, HttpServletRequest request) {
        String clientKey = request.getRemoteAddr();
        return Result.success(songService.playUrl(id, AuthContext.userId(), clientKey));
    }
}
