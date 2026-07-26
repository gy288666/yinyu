package com.yinyu.controller.admin;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.dto.AlbumRequest;
import com.yinyu.dto.CategoryRequest;
import com.yinyu.dto.SingerRequest;
import com.yinyu.entity.Album;
import com.yinyu.entity.Category;
import com.yinyu.entity.Singer;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台内容资源 CRUD：歌手/专辑/分类（api.md 18.4）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;

    // ---------------- 歌手 ----------------

    @GetMapping("/singers")
    @RequireAdmin(permission = "singer:list")
    public Result<PageResult<Singer>> singerPage(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String name) {
        return Result.success(adminContentService.singerPage(pageNum, pageSize, name));
    }

    @PostMapping("/singers")
    @RequireAdmin(permission = "singer:add")
    public Result<Singer> singerCreate(@Valid @RequestBody SingerRequest req) {
        return Result.success(adminContentService.singerCreate(req));
    }

    @PutMapping("/singers/{id}")
    @RequireAdmin(permission = "singer:edit")
    public Result<Void> singerUpdate(@PathVariable Long id, @Valid @RequestBody SingerRequest req) {
        adminContentService.singerUpdate(id, req);
        return Result.success();
    }

    @DeleteMapping("/singers/{id}")
    @RequireAdmin(permission = "singer:delete")
    public Result<Void> singerDelete(@PathVariable Long id) {
        adminContentService.singerDelete(id);
        return Result.success();
    }

    // ---------------- 专辑 ----------------

    @GetMapping("/albums")
    @RequireAdmin(permission = "album:list")
    public Result<PageResult<Album>> albumPage(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) Long singerId) {
        return Result.success(adminContentService.albumPage(pageNum, pageSize, name, singerId));
    }

    @PostMapping("/albums")
    @RequireAdmin(permission = "album:add")
    public Result<Album> albumCreate(@Valid @RequestBody AlbumRequest req) {
        return Result.success(adminContentService.albumCreate(req));
    }

    @PutMapping("/albums/{id}")
    @RequireAdmin(permission = "album:edit")
    public Result<Void> albumUpdate(@PathVariable Long id, @Valid @RequestBody AlbumRequest req) {
        adminContentService.albumUpdate(id, req);
        return Result.success();
    }

    @DeleteMapping("/albums/{id}")
    @RequireAdmin(permission = "album:delete")
    public Result<Void> albumDelete(@PathVariable Long id) {
        adminContentService.albumDelete(id);
        return Result.success();
    }

    // ---------------- 分类 ----------------

    @GetMapping("/categories")
    @RequireAdmin(permission = "category:list")
    public Result<List<Map<String, Object>>> categoryTree() {
        return Result.success(adminContentService.categoryTree());
    }

    @PostMapping("/categories")
    @RequireAdmin(permission = "category:add")
    public Result<Category> categoryCreate(@Valid @RequestBody CategoryRequest req) {
        return Result.success(adminContentService.categoryCreate(req));
    }

    @PutMapping("/categories/{id}")
    @RequireAdmin(permission = "category:edit")
    public Result<Void> categoryUpdate(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        adminContentService.categoryUpdate(id, req);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    @RequireAdmin(permission = "category:delete")
    public Result<Void> categoryDelete(@PathVariable Long id) {
        adminContentService.categoryDelete(id);
        return Result.success();
    }
}
