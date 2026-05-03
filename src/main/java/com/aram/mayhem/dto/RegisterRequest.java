package com.aram.mayhem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    @Size(max = 128, message = "email must be at most 128 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 64, message = "password must be between 6 and 64 characters")
    private String password;

    @NotBlank(message = "nickname is required")
    @Size(min = 2, max = 64, message = "nickname must be between 2 and 64 characters")
    private String nickname;
}
