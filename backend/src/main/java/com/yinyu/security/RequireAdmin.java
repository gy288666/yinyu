package com.yinyu.security;

import java.lang.annotation.*;

/**
 * 需要后台管理员登录（管理员 token 体系），可附带 RBAC 权限标识
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {

    /** 权限标识，如 music:audit；空串表示仅要求登录。SUPER_ADMIN 角色放行全部 */
    String permission() default "";
}
