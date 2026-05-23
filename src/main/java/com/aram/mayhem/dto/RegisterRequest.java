package com.aram.mayhem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求对象 —— 前端发送注册请求时携带的数据
 *
 * 和 LoginRequest 相比，多了 nickname 字段和更严格的密码长度限制。
 * 注册时需要提供更多信息，登录时只需要邮箱和密码。
 *
 * 参数校验注解说明：
 * - @NotBlank：不能为空
 * - @Email：必须是合法邮箱格式
 * - @Size(min, max)：字符串长度限制
 *
 * 数据流向：
 * 前端注册页 → POST /api/auth/register（携带 RegisterRequest 的 JSON）
 * → AuthController → AuthService
 *
 * 关联类：
 * - AuthController：接收注册请求
 * - AuthService：处理注册逻辑（检查邮箱唯一性、加密密码、保存用户）
 */
@Data
public class RegisterRequest {

    /**
     * 注册邮箱 —— 必须唯一，不能与已有用户重复
     *
     * @NotBlank：不能为空
     * @Email：必须是合法邮箱格式
     * @Size(max = 128)：最长 128 个字符，防止超长输入
     */
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    @Size(max = 128, message = "email must be at most 128 characters")
    private String email;

    /**
     * 注册密码 —— 至少 6 位，最长 64 位
     *
     * @NotBlank：不能为空
     * @Size(min = 6, max = 64)：密码长度 6~64 位
     * 密码会在后端用 BCrypt 加密后存储，永远不存明文
     */
    @NotBlank(message = "password is required")
    @Size(min = 6, max = 64, message = "password must be between 6 and 64 characters")
    private String password;

    /**
     * 用户昵称 —— 显示在社区中的名称
     *
     * @NotBlank：不能为空
     * @Size(min = 2, max = 64)：昵称长度 2~64 位
     * 太短的昵称不好辨认，太长的昵称影响显示
     */
    @NotBlank(message = "nickname is required")
    @Size(min = 2, max = 64, message = "nickname must be between 2 and 64 characters")
    private String nickname;
}
