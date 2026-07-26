package com.yinyu.controller.admin;

import com.yinyu.common.result.Result;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台评论管理（api.md 13.3 管理员等价删除接口）
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @DeleteMapping("/{id}")
    @RequireAdmin(permission = "comment:delete")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id, null, true);
        return Result.success();
    }
}
