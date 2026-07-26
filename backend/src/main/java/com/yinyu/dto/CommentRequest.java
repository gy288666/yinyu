package com.yinyu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发表评论/回复请求（api.md 13.2） */
@Data
public class CommentRequest {

    /** SONG / PLAYLIST / ALBUM */
    @NotBlank(message = "不能为空")
    private String targetType;

    @NotNull(message = "不能为空")
    private Long targetId;

    @NotBlank(message = "不能为空")
    @Size(max = 500, message = "长度不能超过500字")
    private String content;

    /** 回复时传被回复评论 ID */
    private Long parentId;
}
