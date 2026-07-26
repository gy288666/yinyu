package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.exception.BizException;
import com.yinyu.dto.LoginRequest;
import com.yinyu.dto.RegisterRequest;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.User;
import com.yinyu.entity.UserLikeSong;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.UserLikeSongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.security.JwtUtil;
import com.yinyu.security.TokenBlacklistService;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.TokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户用户认证：注册/登录/登出/当前信息
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final UserMapper userMapper;
    private final UserLikeSongMapper userLikeSongMapper;
    private final PlaylistMapper playlistMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final StringRedisTemplate redis;
    private final MinioUtil minioUtil;

    public TokenVO register(RegisterRequest req) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user.setNickname(req.getNickname());
        user.setRegisterChannel(req.getChannel() == null ? "direct" : req.getChannel());
        user.setGender(0);
        user.setExp(0L);
        user.setIsVip(0);
        user.setStatus(1);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildTokenVO(user, false);
    }

    public TokenVO login(LoginRequest req) {
        String lockKey = RedisKeys.LOGIN_LOCK_PREFIX + "USER:" + req.getUsername();
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED, "账号已锁定，请10分钟后再试");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            recordFail("USER", req.getUsername());
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
        }
        redis.delete(RedisKeys.LOGIN_FAIL_PREFIX + "USER:" + req.getUsername());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return buildTokenVO(user, true);
    }

    /** 连续错 5 次锁 10 分钟 */
    private void recordFail(String type, String username) {
        String failKey = RedisKeys.LOGIN_FAIL_PREFIX + type + ":" + username;
        Long count = redis.opsForValue().increment(failKey);
        redis.expire(failKey, LOCK_DURATION);
        if (count != null && count >= MAX_FAIL) {
            redis.opsForValue().set(RedisKeys.LOGIN_LOCK_PREFIX + type + ":" + username,
                    "1", LOCK_DURATION);
            redis.delete(failKey);
        }
    }

    private TokenVO buildTokenVO(User user, boolean withVip) {
        TokenVO vo = new TokenVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAccessToken(jwtUtil.createAccessToken(JwtUtil.TYPE_USER, user.getId()));
        vo.setRefreshToken(jwtUtil.createRefreshToken(JwtUtil.TYPE_USER, user.getId()));
        vo.setExpiresIn(jwtUtil.getAccessExpireSeconds());
        if (withVip) {
            vo.setVip(isVipActive(user));
            vo.setVipExpireAt(user.getVipExpireTime());
        }
        return vo;
    }

    public static boolean isVipActive(User user) {
        return user.getIsVip() != null && user.getIsVip() == 1
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    /** 登出：accessToken 入 Redis 黑名单（剩余有效期） */
    public void logout(String token) {
        if (token != null) {
            blacklistService.blacklist(token, jwtUtil.getAccessExpireSeconds());
        }
    }

    /** 当前用户信息（api.md 2.5） */
    public Map<String, Object> me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
        }
        Long likeCount = userLikeSongMapper.selectCount(
                new LambdaQueryWrapper<UserLikeSong>().eq(UserLikeSong::getUserId, userId));
        Long playlistCount = playlistMapper.selectCount(
                new LambdaQueryWrapper<Playlist>().eq(Playlist::getUserId, userId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", minioUtil.publicImageUrl(user.getAvatar()));
        data.put("gender", user.getGender());
        data.put("signature", user.getSignature());
        data.put("level", user.getLevelId());
        data.put("vip", isVipActive(user));
        data.put("vipExpireAt", user.getVipExpireTime());
        data.put("likeCount", likeCount);
        data.put("playlistCount", playlistCount);
        return data;
    }

    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
        }
        return user;
    }
}
