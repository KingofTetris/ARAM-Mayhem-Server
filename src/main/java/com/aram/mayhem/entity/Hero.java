package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_hero")
public class Hero {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer riotId;

    private String nameEn;

    private String nameZh;

    private String title;

    private String role;

    private String imageUrl;

    private String tier;

    private BigDecimal winRate;

    private BigDecimal pickRate;

    private String confidenceLevel;

    private String version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
