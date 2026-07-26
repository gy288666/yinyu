package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.service.PlaylistService;
import com.yinyu.vo.PlaylistDetailVO;
import com.yinyu.vo.PlaylistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门户歌单广场（api.md 6.1-6.3）
 */
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public Result<PageResult<PlaylistVO>> square(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String tag,
                                                 @RequestParam(defaultValue = "hot") String sort) {
        return Result.success(playlistService.square(pageNum, pageSize, tag, sort));
    }

    @GetMapping("/hot")
    public Result<List<PlaylistVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(playlistService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<PlaylistDetailVO> detail(@PathVariable Long id) {
        return Result.success(playlistService.detail(id, AuthContext.userId()));
    }
}
