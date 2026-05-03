package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_strategy")
public class Strategy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long heroId;

    private String title;

    private String description;

    private Integer upvotes;

    private Integer downvotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
