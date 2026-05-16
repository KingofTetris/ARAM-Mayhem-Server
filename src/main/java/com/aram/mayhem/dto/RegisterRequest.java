package com.aram.mayhem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求对象
 *
 * 数据流向：前端注册页 → AuthController → AuthService
 */
@Data
public class RegisterRequest {

    /** 注册邮箱（最长128字） */
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    @Size(max = 128, message = "email must be at most 128 characters")
    private String email;

    /** 注册密码（6~64字） */
    @NotBlank(message = "password is required")
    @Size(min = 6, max = 64, message = "password must be between 6 and 64 characters")
    private String password;

    /** 用户昵称（2~64字） */
    @NotBlank(message = "nickname is required")
    @Size(min = 2, max = 64, message = "nickname must be between 2 and 64 characters")
    private String nickname;
}
