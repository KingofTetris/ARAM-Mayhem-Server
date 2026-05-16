package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应对象
 *
 * 数据流向：AuthService → AuthController → 前端登录/注册页
 * 用途：登录/注册成功后返回用户信息和 JWT Token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** 用户 ID */
    private Long userId;
    /** 用户邮箱 */
    private String email;
    /** 用户昵称 */
    private String nickname;
    /** 用户角色（ROLE_USER / ROLE_ADMIN） */
    private String role;
    /** 访问令牌（短期有效，如 15 分钟） */
    private String accessToken;
    /** 刷新令牌（长期有效，如 7 天） */
    private String refreshToken;
}
