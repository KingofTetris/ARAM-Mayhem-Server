package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_vote")
public class Vote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long strategyId;

    private String voteType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
