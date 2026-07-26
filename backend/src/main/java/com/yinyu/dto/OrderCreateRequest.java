package com.yinyu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建订单请求（api.md 15.2） */
@Data
public class OrderCreateRequest {

    /** VIP / SONG */
    @NotBlank(message = "不能为空")
    private String orderType;

    /** orderType=VIP 时必填 */
    private Long planId;

    /** orderType=SONG 时必填 */
    private Long songId;
}
