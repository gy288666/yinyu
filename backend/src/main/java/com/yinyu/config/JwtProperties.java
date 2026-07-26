package com.yinyu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（用户/管理员双密钥）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String userSecret;
    private String adminSecret;
    private long accessExpireSeconds = 7200;
    private long refreshExpireSeconds = 604800;
}
