package com.yinyu.controller;

import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.RecommendService;
import com.yinyu.vo.PlaylistVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐 / 私人FM（api.md 10.1-10.5）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /** 每日推荐（游客返回全站热门版） */
    @GetMapping("/recommend/daily")
    public Result<Map<String, Object>> daily() {
        return Result.success(recommendService.daily(AuthContext.userId()));
    }

    /** 为你推荐 */
    @GetMapping("/recommend/songs")
    public Result<List<SongVO>> songs(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(recommendService.songs(limit));
    }

    /** 推荐歌单 */
    @GetMapping("/recommend/playlists")
    public Result<List<PlaylistVO>> playlists(@RequestParam(defaultValue = "6") int limit) {
        return Result.success(recommendService.playlists(limit));
    }

    /** 私人FM 下一曲 */
    @GetMapping("/fm/next")
    @RequireUser
    public Result<SongVO> fmNext() {
        return Result.success(recommendService.fmNext(AuthContext.userId()));
    }

    /** 私人FM 不喜欢 */
    @PostMapping("/fm/dislike")
    @RequireUser
    public Result<Void> fmDislike(@RequestBody Map<String, Object> body) {
        Long songId = body.get("songId") == null ? null
                : Long.valueOf(String.valueOf(body.get("songId")));
        recommendService.fmDislike(AuthContext.userId(), songId);
        return Result.success();
    }
}
