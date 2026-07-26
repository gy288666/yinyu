package com.yinyu.controller;

import com.yinyu.common.result.Result;
import com.yinyu.dto.LoginRequest;
import com.yinyu.dto.RegisterRequest;
import com.yinyu.security.AuthContext;
import com.yinyu.security.RequireUser;
import com.yinyu.service.AuthService;
import com.yinyu.vo.TokenVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 门户用户认证（api.md 2.1-2.5）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @PostMapping("/logout")
    @RequireUser
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        authService.logout(header != null && header.startsWith("Bearer ") ? header.substring(7) : null);
        return Result.success();
    }

    @GetMapping("/me")
    @RequireUser
    public Result<Map<String, Object>> me() {
        return Result.success(authService.me(AuthContext.userId()));
    }
}
