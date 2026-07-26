package com.yinyu.security;

import com.yinyu.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * accessToken 黑名单（登出后剩余有效期内拉黑）
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redis;

    private String key(String token) {
        return RedisKeys.JWT_BLACKLIST_PREFIX
                + DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }

    public void blacklist(String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(key(token), "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(key(token)));
    }
}
