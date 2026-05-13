package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "tb_augment", autoResultMap = true)
public class Augment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nameZh;

    private String nameEn;

    private String description;

    private String quality;

    private String synergySet;

    private String synergySet2;

    private String synergySet3;

    private String iconUrl;

    private BigDecimal winRate;

    private BigDecimal pickRate;

    private BigDecimal avgPlacement;

    private String tier;

    private Boolean isTrap;

    private String version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
