package com.yinyu.security;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一鉴权拦截器：
 * 1. 总是尝试解析 Authorization（软登录，公开接口可拿到当前用户返回 liked 等字段）
 * 2. @RequireUser：必须携带有效用户 token（管理员 token 访问用户接口视为无效）
 * 3. @RequireAdmin：必须携带有效管理员 token，且通过 RBAC 权限码校验
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;
    private final AdminPermissionService adminPermissionService;
    private final UserStatusChecker userStatusChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthContext.clear();
        String token = extractToken(request);

        RequireUser requireUser = null;
        RequireAdmin requireAdmin = null;
        if (handler instanceof HandlerMethod hm) {
            requireUser = find(hm, RequireUser.class);
            requireAdmin = find(hm, RequireAdmin.class);
        }

        // 软解析：无论是否要求登录，先尝试识别身份
        if (token != null && !blacklist.isBlacklisted(token)) {
            Claims userClaims = jwtUtil.parse(token, JwtUtil.TYPE_USER);
            if (userClaims != null && JwtUtil.KIND_ACCESS.equals(userClaims.get("kind", String.class))) {
                AuthContext.setUserId(Long.valueOf(userClaims.getSubject()));
            } else {
                Claims adminClaims = jwtUtil.parse(token, JwtUtil.TYPE_ADMIN);
                if (adminClaims != null && JwtUtil.KIND_ACCESS.equals(adminClaims.get("kind", String.class))) {
                    AuthContext.setAdminId(Long.valueOf(adminClaims.getSubject()));
                }
            }
        }

        if (requireUser != null) {
            if (AuthContext.userId() == null) {
                // 管理员 token 调用用户接口 -> 403；无 token/无效 token -> 401
                if (AuthContext.adminId() != null) {
                    throw new BizException(ErrorCode.FORBIDDEN, "无操作权限");
                }
                throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
            }
            // 后台停用后即时生效（Redis 停用标记）
            if (userStatusChecker.isDisabled(AuthContext.userId())) {
                throw new BizException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
            }
        }

        if (requireAdmin != null) {
            if (AuthContext.adminId() == null) {
                if (AuthContext.userId() != null) {
                    throw new BizException(ErrorCode.FORBIDDEN, "无操作权限");
                }
                throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
            }
            if (!adminPermissionService.hasPermission(AuthContext.adminId(), requireAdmin.permission())) {
                throw new BizException(ErrorCode.FORBIDDEN, "无操作权限");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        // 兜底：支持 ?token= 查询参数（CSV 导出等 window.open 下载场景）
        String queryToken = request.getParameter("token");
        return queryToken == null || queryToken.isBlank() ? null : queryToken.trim();
    }

    private <A extends java.lang.annotation.Annotation> A find(HandlerMethod hm, Class<A> type) {
        A a = hm.getMethodAnnotation(type);
        return a != null ? a : hm.getBeanType().getAnnotation(type);
    }
}
