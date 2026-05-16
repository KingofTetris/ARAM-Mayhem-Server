package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应表：tb_user
 * 数据流向：MyBatis-Plus ↔ AuthService ↔ AuthController → AuthResponse
 * 关联：AuthController, AuthService, CustomUserDetailsService, UserMapper
 */
@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录邮箱（唯一） */
    private String email;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像 URL */
    private String avatarUrl;

    /** 显示模式（0=浅色, 1=深色） */
    private Integer displayMode;

    /** 是否启用通知（1=启用, 0=关闭） */
    private Integer notificationEnabled;

    /** 用户角色（ROLE_USER / ROLE_ADMIN） */
    private String role;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（1=已删除, 0=正常） */
    @TableLogic
    private Integer deleted;
}
