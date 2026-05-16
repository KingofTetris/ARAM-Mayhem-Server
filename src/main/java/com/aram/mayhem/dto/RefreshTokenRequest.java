package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求对象
 *
 * 数据流向：前端 → AuthController → AuthService
 * 用途：使用 refreshToken 换取新的 accessToken
 */
@Data
public class RefreshTokenRequest {

    /** 刷新令牌 */
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
