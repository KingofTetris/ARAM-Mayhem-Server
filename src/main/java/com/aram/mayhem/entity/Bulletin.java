package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_bulletin")
public class Bulletin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String title;

    private String content;

    private String imageUrl;

    private Integer isPinned;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
