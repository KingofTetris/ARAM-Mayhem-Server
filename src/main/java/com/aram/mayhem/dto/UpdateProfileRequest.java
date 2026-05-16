package com.aram.mayhem.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料更新请求对象
 *
 * 数据流向：前端个人中心页 → UserController → UserService
 * 用途：用户修改个人资料（昵称、头像、显示模式、通知开关）
 * 校验：昵称长度 1-30 位
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /** 用户昵称（1-30位） */
    @Size(min = 1, max = 30, message = "昵称长度必须在1-30位之间")
    private String nickname;

    /** 用户头像 URL */
    private String avatarUrl;

    /** 显示模式（0=浅色, 1=深色） */
    private Integer displayMode;

    /** 是否启用通知（1=启用, 0=关闭） */
    private Integer notificationEnabled;
}
