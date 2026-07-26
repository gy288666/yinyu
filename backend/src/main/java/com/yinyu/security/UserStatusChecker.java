package com.yinyu.security;

import com.yinyu.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 门户用户停用标记：后台停用用户时写入 Redis，使已签发 token 即时失效
 */
@Component
@RequiredArgsConstructor
public class UserStatusChecker {

    /** 与 refreshToken 最长有效期一致，标记过期后由登录校验兜底 */
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    public boolean isDisabled(Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(RedisKeys.USER_DISABLED_PREFIX + userId));
    }

    public void markDisabled(Long userId) {
        redis.opsForValue().set(RedisKeys.USER_DISABLED_PREFIX + userId, "1", TTL);
    }

    public void markEnabled(Long userId) {
        redis.delete(RedisKeys.USER_DISABLED_PREFIX + userId);
    }
}
