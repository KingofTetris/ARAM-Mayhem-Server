package com.aram.mayhem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求对象
 *
 * 数据流向：前端登录页 → AuthController → AuthService
 */
@Data
public class LoginRequest {

    /** 登录邮箱 */
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    /** 登录密码 */
    @NotBlank(message = "password is required")
    private String password;
}
