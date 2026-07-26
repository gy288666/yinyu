package com.yinyu.security;

import java.lang.annotation.*;

/**
 * 需要门户用户登录（用户 token 体系）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireUser {
}
