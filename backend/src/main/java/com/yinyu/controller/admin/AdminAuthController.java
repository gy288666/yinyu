package com.yinyu.controller.admin;

import com.yinyu.common.result.Result;
import com.yinyu.dto.AdminLoginRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台管理员认证（api.md 2.9-2.12）
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        return Result.success(adminAuthService.captcha());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest req) {
        return Result.success(adminAuthService.login(req));
    }

    @PostMapping("/logout")
    @RequireAdmin
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        adminAuthService.logout(header != null && header.startsWith("Bearer ") ? header.substring(7) : null);
        return Result.success();
    }

    @GetMapping("/me")
    @RequireAdmin
    public Result<Map<String, Object>> me() {
        return Result.success(adminAuthService.me(AuthContext.adminId()));
    }
}
