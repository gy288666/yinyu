package com.yinyu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台分类新增/修改（api.md 18.4.17/18.4.18）
 */
@Data
public class CategoryRequest {

    @Size(max = 50, message = "分类名过长")
    private String name;

    private Long parentId;
    private Integer sort;
    private Boolean enabled;
}
