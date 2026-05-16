package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 强化符文实体类
 *
 * 对应表：tb_augment
 * 数据流向：MyBatis-Plus ↔ AugmentService ↔ AugmentController → AugmentVO/AugmentListVO
 * 关联：AugmentController, AugmentService, AugmentMapper
 */
@Data
@TableName(value = "tb_augment", autoResultMap = true)
public class Augment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 符文中文名 */
    private String nameZh;

    /** 符文英文名 */
    private String nameEn;

    /** 符文描述/效果说明 */
    private String description;

    /** 符文品质（如 银色/金色/棱彩） */
    private String quality;

    /** 第一套装名称（如 精密） */
    private String synergySet;

    /** 第二套装名称（可为 null） */
    private String synergySet2;

    /** 第三套装名称（可为 null） */
    private String synergySet3;

    /** 符文图标 URL */
    private String iconUrl;

    /** ARAM 胜率（0~1 之间） */
    private BigDecimal winRate;

    /** ARAM 选取率（0~1 之间） */
    private BigDecimal pickRate;

    /** 平均排名（1~8 之间，越小越好） */
    private BigDecimal avgPlacement;

    /** 梯级评级（S+/S/A/B/C） */
    private String tier;

    /** 是否为陷阱符文（数据表现差，不推荐） */
    private Boolean isTrap;

    /** 是否为版本陷阱符文（被大幅削弱，慎用） */
    private Boolean isVersionTrap;

    /** 版本陷阱标记时间，取消标记时置 null */
    private LocalDateTime versionTrapSince;

    /** 数据版本号（如 14.8） */
    private String version;

    /** 数据更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
