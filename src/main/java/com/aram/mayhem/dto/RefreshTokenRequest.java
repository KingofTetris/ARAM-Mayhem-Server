package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求对象 —— 前端用 refreshToken 换取新的 accessToken 时发送
 *
 * 当 accessToken 过期后，前端不需要让用户重新登录，
 * 而是发送这个请求，用 refreshToken 获取新的 accessToken。
 *
 * 就像酒店房卡过期了，你不需要重新办理入住，
 * 只需要拿着预订确认信（refreshToken）去前台换一张新房卡（accessToken）。
 *
 * 数据流向：
 * 前端 → POST /api/auth/refresh（携带 RefreshTokenRequest 的 JSON）
 * → AuthController → AuthService.refreshToken()
 *
 * 关联类：
 * - AuthController：接收刷新请求
 * - AuthService：验证 refreshToken 并生成新的 accessToken
 */
@Data
public class RefreshTokenRequest {

    /**
     * 刷新令牌 —— 登录时返回的 refreshToken
     *
     * @NotBlank(message = "refreshToken is required")：刷新令牌不能为空
     * 后端会验证这个 Token 的签名、过期时间和 type=refresh 标记
     */
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
