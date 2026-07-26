package com.yinyu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.result.Result;
import com.yinyu.entity.Category;
import com.yinyu.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类树（api.md 7.1）：仅启用项，两级
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryMapper categoryMapper;

    @GetMapping
    public Result<List<Map<String, Object>>> tree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1).orderByAsc(Category::getSort));
        List<Map<String, Object>> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(root -> {
                    Map<String, Object> node = new LinkedHashMap<String, Object>();
                    node.put("id", root.getId());
                    node.put("name", root.getName());
                    node.put("children", all.stream()
                            .filter(c -> root.getId().equals(c.getParentId()))
                            .map(c -> {
                                Map<String, Object> child = new LinkedHashMap<String, Object>();
                                child.put("id", c.getId());
                                child.put("name", c.getName());
                                return child;
                            }).toList());
                    return node;
                }).toList();
        return Result.success(roots);
    }
}
