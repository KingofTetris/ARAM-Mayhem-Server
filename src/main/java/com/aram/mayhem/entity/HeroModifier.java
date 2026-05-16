package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 英雄属性修正器实体类
 *
 * 对应表：tb_hero_modifier
 * 数据流向：MyBatis-Plus ↔ HeroService → HeroDetailVO.modifiers
 * 关联：HeroService, HeroModifierMapper
 * 用途：存储 ARAM 地图对英雄属性的修正值（如伤害倍率、生命值加成等）
 */
@Data
@TableName("tb_hero_modifier")
public class HeroModifier {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联英雄 ID */
    private Long heroId;

    /** 修正器名称（如 伤害倍率、生命值加成） */
    private String modifierName;

    /** 修正值（如 0.9 表示 90% 倍率） */
    private BigDecimal modifierValue;
}
