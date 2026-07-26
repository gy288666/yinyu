package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.service.SingerService;
import com.yinyu.vo.AlbumVO;
import com.yinyu.vo.SingerVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 门户歌手接口（api.md 4.1-4.4）
 */
@RestController
@RequestMapping("/api/singers")
@RequiredArgsConstructor
public class SingerController {

    private final SingerService singerService;

    @GetMapping
    public Result<PageResult<SingerVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String area,
                                             @RequestParam(required = false) Integer type,
                                             @RequestParam(required = false) String initial) {
        return Result.success(singerService.page(pageNum, pageSize, area, type, initial));
    }

    @GetMapping("/{id}")
    public Result<SingerVO> detail(@PathVariable Long id) {
        return Result.success(singerService.detail(id));
    }

    @GetMapping("/{id}/songs")
    public Result<PageResult<SongVO>> songs(@PathVariable Long id,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(singerService.songs(id, pageNum, pageSize));
    }

    @GetMapping("/{id}/albums")
    public Result<PageResult<AlbumVO>> albums(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(singerService.albums(id, pageNum, pageSize));
    }
}
