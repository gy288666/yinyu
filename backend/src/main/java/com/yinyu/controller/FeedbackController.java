package com.yinyu.controller;

import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.dto.FeedbackRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户反馈（api.md 17.x）
 */
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @RequireUser
    public Result<Map<String, Object>> create(@Valid @RequestBody FeedbackRequest req) {
        return Result.success(feedbackService.create(req, AuthContext.userId()));
    }

    @GetMapping
    @RequireUser
    public Result<PageResult<Map<String, Object>>> myPage(@RequestParam(defaultValue = "1") long pageNum,
                                                          @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(feedbackService.myPage(AuthContext.userId(), pageNum, pageSize));
    }
}
