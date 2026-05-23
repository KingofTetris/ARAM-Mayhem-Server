package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应对象 —— 登录/注册成功后返回给前端的数据
 *
 * 这个对象包含了前端需要的所有认证信息：
 * - 用户基本信息（ID、邮箱、昵称、角色）
 * - JWT Token（访问令牌 + 刷新令牌）
 *
 * 前端收到这个响应后，会：
 * 1. 把 accessToken 和 refreshToken 存储在本地（SharedPreferences）
 * 2. 后续 API 请求在 Header 中带上 accessToken
 * 3. accessToken 过期后，用 refreshToken 获取新的 accessToken
 *
 * @Builder 注解：使用建造者模式创建对象，如 AuthResponse.builder().userId(1L).email("a@b.com")...build()
 * 好处：参数多时不会搞错顺序，代码更易读
 *
 * 数据流向：
 * AuthService → AuthController → 前端登录/注册页
 *
 * 关联类：
 * - AuthService：生成此响应
 * - JwtTokenProvider：生成 accessToken 和 refreshToken
 */
@Data
@Builder // 建造者模式，方便构建多字段对象
@NoArgsConstructor // Jackson 反序列化需要
@AllArgsConstructor // Builder 需要全参构造函数
public class AuthResponse {

    /** 用户 ID —— 前端用于标识当前登录用户 */
    private Long userId;

    /** 用户邮箱 —— 前端个人中心页显示 */
    private String email;

    /** 用户昵称 —— 前端社区中显示的用户名 */
    private String nickname;

    /** 用户角色 —— ROLE_USER 或 ROLE_ADMIN，前端根据角色显示/隐藏管理功能 */
    private String role;

    /**
     * 访问令牌（Access Token）—— 短期有效（默认 2 小时）
     * 前端每次 API 请求都要在 Authorization 头中带上这个 Token
     * 格式：Authorization: Bearer <accessToken>
     */
    private String accessToken;

    /**
     * 刷新令牌（Refresh Token）—— 长期有效（默认 7 天）
     * 当 accessToken 过期时，用这个 Token 获取新的 accessToken
     * 不需要用户重新输入密码登录
     */
    private String refreshToken;
}
