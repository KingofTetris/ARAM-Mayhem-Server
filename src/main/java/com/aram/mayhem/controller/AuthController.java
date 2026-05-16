package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.AuthResponse;
import com.aram.mayhem.dto.LoginRequest;
import com.aram.mayhem.dto.RefreshTokenRequest;
import com.aram.mayhem.dto.RegisterRequest;
import com.aram.mayhem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
     * 认证控制器
     *
     * 路径前缀：/api/auth
     * 权限：公开访问（登录/注册/刷新令牌无需认证）
     * 功能：用户注册、用户登录、令牌刷新
     * 关联：AuthService, LoginRequest, RegisterRequest, RefreshTokenRequest, AuthResponse
     */
@Tag(name = "Auth", description = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册（认证模块）
     *
     * 作用：创建新用户账号，验证邮箱唯一性，密码加密存储
     * 密码规则：至少8位，包含字母和数字
     *
     * @param request 注册请求体（邮箱、密码、昵称）
     * @return Result<AuthResponse> 注册成功返回Token信息，失败返回错误信息
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新账号，需要邮箱、密码和昵称")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 调用Service层执行注册逻辑（含密码加密、邮箱唯一性校验）
        AuthResponse response = authService.register(request);
        return Result.success(response);
    }

    /**
     * 用户登录（认证模块）
     *
     * 作用：用户身份验证，验证邮箱密码正确性，返回Access Token和Refresh Token
     * Token有效期：Access Token 15分钟，Refresh Token 7天
     *
     * @param request 登录请求体（邮箱、密码）
     * @return Result<AuthResponse> 登录成功返回Token信息，失败返回错误信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "邮箱密码验证，返回访问令牌和刷新令牌")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 调用Service层执行登录逻辑（含密码校验、Token生成）
        AuthResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 刷新访问令牌（认证模块）
     *
     * 作用：使用有效的Refresh Token换取新的Access Token和Refresh Token
     * 安全机制：刷新后旧Refresh Token失效，防止Token泄漏被滥用
     *
     * @param request 刷新请求体（Refresh Token）
     * @return Result<AuthResponse> 刷新成功返回新Token信息，失败返回错误信息
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用Refresh Token换取新的Access Token")
    public Result<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        // 调用Service层执行Token刷新逻辑（含Refresh Token校验、新Token生成）
        AuthResponse response = authService.refreshToken(request);
        return Result.success(response);
    }
}
