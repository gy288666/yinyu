package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.AuthContext;
import com.yinyu.service.AlbumService;
import com.yinyu.vo.AlbumVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门户专辑接口（api.md 5.1-5.3）
 */
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @GetMapping
    public Result<PageResult<AlbumVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Long singerId) {
        return Result.success(albumService.page(pageNum, pageSize, singerId));
    }

    @GetMapping("/{id}")
    public Result<AlbumVO> detail(@PathVariable Long id) {
        return Result.success(albumService.detail(id, AuthContext.userId()));
    }

    @GetMapping("/{id}/songs")
    public Result<List<SongVO>> songs(@PathVariable Long id) {
        return Result.success(albumService.songs(id));
    }
}
