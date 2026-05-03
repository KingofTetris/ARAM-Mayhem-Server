package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String password;

    private String nickname;

    private String avatarUrl;

    private Integer displayMode;

    private Integer notificationEnabled;

    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
