package com.yinyu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户注册/登录响应（api.md 2.1/2.2）
 */
@Data
public class TokenVO {

    private Long userId;
    private String nickname;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean vip;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime vipExpireAt;
}
