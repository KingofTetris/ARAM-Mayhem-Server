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
 * 用户控制器 —— 个人中心模块的"前台接待处"，处理用户资料相关的 HTTP 请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是个人中心模块的 REST API 入口，提供两个核心接口：
 * 1. getUserProfile()  → 获取当前用户的个人资料
 * 2. updateProfile()   → 更新当前用户的个人资料（部分更新）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、API 接口一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法              | HTTP方法 | 路径                  | 权限     | 说明
 * ------------------|---------|----------------------|---------|------------------
 * getUserProfile()  | GET     | /api/users/me/profile| 需登录  | 获取个人资料
 * updateProfile()   | PATCH   | /api/users/me        | 需登录  | 更新个人资料
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、用户身份获取方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 StrategyController 不同，本 Controller 使用 authentication.getName() 获取用户ID。
 * 这是因为 JwtAuthenticationFilter 在创建 Authentication 时，
 * 将用户ID同时设置为了 name 和 principal，两种方式都可以获取。
 *
 * 关联类：
 * @see com.aram.mayhem.service.UserService 用户服务（实际业务逻辑）
 * @see com.aram.mayhem.common.Result 统一响应包装类
 * @see com.aram.mayhem.dto.UserProfileVO 用户资料视图对象
 * @see com.aram.mayhem.dto.UpdateProfileRequest 更新资料请求对象
 */
@Tag(name = "User", description = "用户接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 构造函数 —— 注入用户服务
     *
     * @param userService 用户服务，负责用户资料的查询和更新业务逻辑
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户资料接口 —— 查询登录用户的完整个人资料
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：GET
     * 路径：/api/users/me/profile
     * 权限：需登录（需要 JWT Token）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 返回数据说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * UserProfileVO 包含：
     * - 基本信息：userId、email、nickname、avatarUrl
     * - 偏好设置：displayMode（浅色/深色）、notificationEnabled（通知开关）
     * - 统计数据：strategyCount（发布的攻略数量）
     *
     * @return Result<UserProfileVO> 用户个人资料
     */
    @Operation(summary = "获取当前用户资料", description = "根据JWT Token获取当前登录用户的个人资料")
    @GetMapping("/me/profile")
    public Result<UserProfileVO> getUserProfile() {
        // 从 SecurityContext 中获取当前登录用户ID
        Long userId = getCurrentUserId();
        // 调用 UserService 获取用户资料
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * 更新当前用户资料接口 —— 部分更新个人资料
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口信息
     * ═══════════════════════════════════════════════════════════════════
     *
     * HTTP 方法：PATCH（部分更新用 PATCH，全量更新用 PUT）
     * 路径：/api/users/me
     * 权限：需登录
     * Content-Type：application/json
     *
     * ═══════════════════════════════════════════════════════════════════
     * 部分更新说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * PATCH 语义：只更新请求中提供的字段，未提供的字段保持不变。
     * 例如：只更新昵称，其他字段不受影响。
     * UserService 内部会检查每个字段是否为 null，只更新非 null 的字段。
     *
     * 可更新字段：
     * - nickname：用户昵称
     * - avatarUrl：头像 URL
     * - displayMode：显示模式（0=浅色, 1=深色）
     * - notificationEnabled：通知开关（1=开启, 0=关闭）
     *
     * 不可更新字段（由系统管理）：
     * - email：登录邮箱（修改邮箱需要验证流程，暂不支持）
     * - password：密码（修改密码需要单独的接口）
     * - role：角色（由管理员分配，用户不能自行修改）
     *
     * @param request 更新资料请求体，只需提供要修改的字段
     * @return Result<UserProfileVO> 更新后的用户资料
     */
    @Operation(summary = "更新当前用户资料", description = "部分更新当前登录用户的个人资料")
    @PatchMapping("/me")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        // 调用 UserService 执行部分更新
        // Service 层会检查每个字段是否为 null，只更新非 null 的字段
        UserProfileVO profile = userService.updateProfile(userId, request);
        return Result.success(profile);
    }

    /**
     * 获取当前登录用户ID —— 从 Spring Security 上下文中提取
     *
     * ═══════════════════════════════════════════════════════════════════
     * 工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. JwtAuthenticationFilter 在请求进入时，从 Authorization 头中提取 JWT Token
     * 2. 解析 Token 获取用户ID，创建 Authentication 对象
     * 3. 将 Authentication 存入 SecurityContextHolder
     * 4. 本方法从 SecurityContextHolder 中取出 Authentication，提取用户ID
     *
     * 注意：本方法使用 authentication.getName() 获取用户ID，
     * 而 StrategyController 使用 authentication.getPrincipal()。
     * 两种方式都可以获取用户ID，因为 JwtAuthenticationFilter 同时设置了两者。
     *
     * @return Long 当前登录用户ID
     */
    private Long getCurrentUserId() {
        // SecurityContextHolder 保存了当前请求的安全上下文
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // getName() 返回的是用户ID字符串（JwtAuthenticationFilter 设置的）
        String userIdStr = authentication.getName();
        // 将字符串转换为 Long 类型
        return Long.parseLong(userIdStr);
    }
}
