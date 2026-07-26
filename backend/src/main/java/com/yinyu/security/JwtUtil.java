package com.yinyu.security;

import com.yinyu.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具：用户 token 与管理员 token 使用不同密钥签发，互不通用
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_ADMIN = "ADMIN";
    public static final String KIND_ACCESS = "access";
    public static final String KIND_REFRESH = "refresh";

    private final JwtProperties props;

    private SecretKey key(String userType) {
        String secret = TYPE_ADMIN.equals(userType) ? props.getAdminSecret() : props.getUserSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userType, Long id) {
        return create(userType, id, KIND_ACCESS, props.getAccessExpireSeconds());
    }

    public String createRefreshToken(String userType, Long id) {
        return create(userType, id, KIND_REFRESH, props.getRefreshExpireSeconds());
    }

    private String create(String userType, Long id, String kind, long expireSeconds) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(id))
                .claim("userType", userType)
                .claim("kind", kind)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(key(userType))
                .compact();
    }

    /**
     * 校验并解析；类型不符或过期返回 null
     */
    public Claims parse(String token, String expectUserType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key(expectUserType)).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectUserType.equals(claims.get("userType", String.class))) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public long getAccessExpireSeconds() {
        return props.getAccessExpireSeconds();
    }
}
