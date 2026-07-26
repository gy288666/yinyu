package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.dto.CommentRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评论（api.md 13.x）
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam String targetType,
                                                        @RequestParam Long targetId,
                                                        @RequestParam(defaultValue = "hot") String sort,
                                                        @RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(commentService.page(targetType, targetId, sort, pageNum, pageSize,
                AuthContext.userId()));
    }

    @PostMapping
    @RequireUser
    public Result<Map<String, Object>> create(@Valid @RequestBody CommentRequest req) {
        return Result.success(commentService.create(req, AuthContext.userId()));
    }

    @DeleteMapping("/{id}")
    @RequireUser
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id, AuthContext.userId(), false);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @RequireUser
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        return Result.success(commentService.like(id, AuthContext.userId()));
    }

    @DeleteMapping("/{id}/like")
    @RequireUser
    public Result<Map<String, Object>> unlike(@PathVariable Long id) {
        return Result.success(commentService.unlike(id, AuthContext.userId()));
    }
}
