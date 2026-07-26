package com.yinyu.controller;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索接口（api.md 8.1-8.3）
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<PageResult<?>> search(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "song") String type,
                                        @RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        if (keyword.isBlank() || keyword.length() > 50) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: keyword 非空且不超过50字");
        }
        return Result.success(searchService.search(keyword.trim(), type, pageNum, pageSize));
    }

    @GetMapping("/hot")
    public Result<List<String>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(searchService.hot(limit));
    }

    @GetMapping("/suggest")
    public Result<List<Map<String, Object>>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }
}
