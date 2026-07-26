package com.yinyu.security;

/**
 * 当前请求认证上下文（ThreadLocal）
 */
public final class AuthContext {

    private AuthContext() {}

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    public static void setUserId(Long id) { USER_ID.set(id); }

    public static void setAdminId(Long id) { ADMIN_ID.set(id); }

    /** 已登录门户用户 ID，未登录为 null（公开接口可据此返回 liked 等个性字段） */
    public static Long userId() { return USER_ID.get(); }

    /** 已登录管理员 ID，未登录为 null */
    public static Long adminId() { return ADMIN_ID.get(); }

    public static void clear() {
        USER_ID.remove();
        ADMIN_ID.remove();
    }
}
