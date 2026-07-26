package com.yinyu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册（api.md 2.1）
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "用户名须为4-20位字母数字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[\\S]{8,32}$", message = "密码须为8-32位且含字母与数字")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不超过20字")
    private String nickname;

    /** 注册渠道 direct/search/share/activity */
    @Pattern(regexp = "^(direct|search|share|activity)$", message = "渠道不合法")
    private String channel = "direct";
}
