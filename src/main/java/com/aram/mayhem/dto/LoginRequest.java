package com.aram.mayhem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求对象 —— 前端发送登录请求时携带的数据
 *
 * DTO（Data Transfer Object）分为两类：
 * - Request DTO：前端发给后端的数据（如 LoginRequest）
 * - Response DTO / VO：后端返回给前端的数据（如 AuthResponse）
 *
 * 参数校验注解说明：
 * - @NotBlank：字段不能为空字符串或 null
 * - @Email：字段必须是合法的邮箱格式
 * 这些注解配合 @Valid 使用，Spring 会自动校验，校验失败抛出 MethodArgumentNotValidException
 *
 * 数据流向：
 * 前端登录页 → POST /api/auth/login（携带 LoginRequest 的 JSON）→ AuthController → AuthService
 *
 * 关联类：
 * - AuthController：接收登录请求
 * - AuthService：处理登录逻辑
 * - RegisterRequest：注册请求对象（结构类似但多了 nickname 字段）
 */
@Data
public class LoginRequest {

    /**
     * 登录邮箱 —— 用户的唯一标识
     *
     * @NotBlank(message = "email is required")：邮箱不能为空
     * @Email(message = "invalid email format")：必须是合法邮箱格式，如 user@example.com
     */
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    /**
     * 登录密码 —— 明文传输，后端收到后与数据库中的 BCrypt 哈希值比对
     *
     * @NotBlank(message = "password is required")：密码不能为空
     * 注意：密码在传输时是明文，但 HTTPS 加密保证了传输安全
     */
    @NotBlank(message = "password is required")
    private String password;
}
