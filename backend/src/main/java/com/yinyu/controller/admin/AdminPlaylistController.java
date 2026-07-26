package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminPlaylistService;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台歌单管理（api.md 18.4.11-18.4.15）
 */
@RestController
@RequestMapping("/api/admin/playlists")
@RequiredArgsConstructor
public class AdminPlaylistController {

    private final AdminPlaylistService adminPlaylistService;

    @GetMapping
    @RequireAdmin(permission = "playlist:list")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize,
                                                        @RequestParam(required = false) String title,
                                                        @RequestParam(required = false) Boolean official) {
        return Result.success(adminPlaylistService.page(pageNum, pageSize, title, official));
    }

    @GetMapping("/{id}")
    @RequireAdmin(permission = "playlist:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(adminPlaylistService.detail(id));
    }

    @PostMapping
    @RequireAdmin(permission = "playlist:add")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success(adminPlaylistService.create(body));
    }

    @PutMapping("/{id}")
    @RequireAdmin(permission = "playlist:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminPlaylistService.update(id, body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin(permission = "playlist:delete")
    public Result<Void> delete(@PathVariable Long id) {
        adminPlaylistService.delete(id);
        return Result.success();
    }

    /** 曲目维护：songIds 有序数组整体替换（加曲/移曲/排序均走此接口） */
    @PutMapping("/{id}/songs")
    @RequireAdmin(permission = "playlist:edit")
    public Result<Map<String, Object>> replaceSongs(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        return Result.success(adminPlaylistService.replaceSongs(id, Params.longList(body, "songIds")));
    }
}
