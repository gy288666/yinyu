package com.yinyu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 提交反馈请求（api.md 17.1） */
@Data
public class FeedbackRequest {

    /** BUG / SUGGEST / COPYRIGHT / OTHER */
    @NotBlank(message = "不能为空")
    private String type;

    @NotBlank(message = "不能为空")
    @Size(max = 1000, message = "长度不能超过1000字")
    private String content;

    /** 截图 URL，最多 3 张 */
    @Size(max = 3, message = "最多3张截图")
    private List<String> images;

    private String contact;
}
