package com.yinyu.controller;

import com.yinyu.common.result.Result;
import com.yinyu.dto.PlaylistRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.PlaylistService;
import com.yinyu.vo.PlaylistVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 我的歌单 CRUD 与曲目维护（api.md 6.4-6.8）
 */
@RestController
@RequestMapping("/api/my/playlists")
@RequiredArgsConstructor
@RequireUser
public class MyPlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public Result<List<PlaylistVO>> list() {
        return Result.success(playlistService.myPlaylists(AuthContext.userId()));
    }

    @PostMapping
    public Result<PlaylistVO> create(@Valid @RequestBody PlaylistRequest req) {
        return Result.success(playlistService.create(AuthContext.userId(),
                req.getTitle(), req.getCover(), req.getIntro(), req.getVisibility()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PlaylistRequest req) {
        playlistService.update(AuthContext.userId(), id,
                req.getTitle(), req.getCover(), req.getIntro(), req.getVisibility());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playlistService.delete(AuthContext.userId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/songs")
    public Result<Map<String, Object>> addSong(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long songId = body.get("songId");
        if (songId == null) {
            return Result.error(com.yinyu.common.constant.ErrorCode.PARAM_INVALID, "参数校验失败: songId 不能为空");
        }
        return Result.success(playlistService.addSong(AuthContext.userId(), id, songId));
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public Result<Map<String, Object>> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        return Result.success(playlistService.removeSong(AuthContext.userId(), id, songId));
    }
}
