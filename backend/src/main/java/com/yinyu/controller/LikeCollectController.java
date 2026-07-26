package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.LikeCollectService;
import com.yinyu.vo.PlaylistVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 喜欢/收藏（api.md 11.1-11.5）
 */
@RestController
@RequiredArgsConstructor
@RequireUser
public class LikeCollectController {

    private final LikeCollectService likeCollectService;

    @PostMapping("/api/likes/songs/{songId}")
    public Result<Map<String, Object>> like(@PathVariable Long songId) {
        return Result.success(likeCollectService.likeSong(AuthContext.userId(), songId));
    }

    @DeleteMapping("/api/likes/songs/{songId}")
    public Result<Map<String, Object>> unlike(@PathVariable Long songId) {
        return Result.success(likeCollectService.unlikeSong(AuthContext.userId(), songId));
    }

    @GetMapping("/api/likes/songs")
    public Result<PageResult<SongVO>> likedSongs(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(likeCollectService.likedSongs(AuthContext.userId(), pageNum, pageSize));
    }

    @PostMapping("/api/collections/playlists/{id}")
    public Result<Void> collect(@PathVariable Long id) {
        likeCollectService.collectPlaylist(AuthContext.userId(), id);
        return Result.success();
    }

    @DeleteMapping("/api/collections/playlists/{id}")
    public Result<Void> uncollect(@PathVariable Long id) {
        likeCollectService.uncollectPlaylist(AuthContext.userId(), id);
        return Result.success();
    }

    @GetMapping("/api/collections")
    public Result<PageResult<PlaylistVO>> collections(@RequestParam(defaultValue = "playlist") String type,
                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(likeCollectService.collections(AuthContext.userId(), type, pageNum, pageSize));
    }
}
