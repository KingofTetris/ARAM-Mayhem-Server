package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_augment")
public class Augment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nameEn;

    private String nameZh;

    private String quality;

    private String description;

    private String synergySet;

    private String imageUrl;

    private Integer isTrap;

    private String confidenceLevel;

    private String version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
