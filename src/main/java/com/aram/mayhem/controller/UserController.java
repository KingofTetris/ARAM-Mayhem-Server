package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * 路径前缀：/api/users
 * 权限：需认证（Bearer Token）
 * 功能：用户资料查询、用户资料更新
 * 关联：UserService, UserProfileVO, UpdateProfileRequest
 */
@Tag(name = "User", description = "用户接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户资料（个人中心模块）
     *
     * 作用：从 JWT Token 中提取用户ID，返回该用户的完整资料信息
     * 包含：基本信息（邮箱/昵称/头像）、偏好设置（显示模式/通知）、统计（攻略数/收藏数）
     *
     * @return Result<UserProfileVO> 用户资料
     */
    @Operation(summary = "获取当前用户资料", description = "根据JWT Token获取当前登录用户的个人资料")
    @GetMapping("/me/profile")
    public Result<UserProfileVO> getUserProfile() {
        Long userId = getCurrentUserId();
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * 更新当前用户资料（个人中心模块）
     *
     * 作用：部分更新当前用户信息，仅更新请求中非 null 的字段
     * 可更新字段：nickname、avatarUrl、displayMode、notificationEnabled
     *
     * @param request 更新请求体
     * @return Result<UserProfileVO> 更新后的用户资料
     */
    @Operation(summary = "更新当前用户资料", description = "部分更新当前登录用户的个人资料")
    @PatchMapping("/me")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        UserProfileVO profile = userService.updateProfile(userId, request);
        return Result.success(profile);
    }

    /**
     * 从 SecurityContext 获取当前登录用户ID（私有方法）
     *
     * 作用：从 JWT 认证上下文中提取用户主体（存储为用户ID字符串）
     *
     * @return Long 当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = authentication.getName();
        return Long.parseLong(userIdStr);
    }
}
