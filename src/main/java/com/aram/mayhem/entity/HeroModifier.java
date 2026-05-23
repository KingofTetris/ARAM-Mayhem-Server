package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 英雄属性修正器实体类 —— 对应数据库中的 tb_hero_modifier 表
 *
 * ARAM 模式下，部分英雄的属性会被调整（增强或削弱），以保持游戏平衡。
 * 比如某个法师的伤害被降低到 90%，某个坦克的生命值增加了 10%。
 * 这些调整信息就存储在这张表中。
 *
 * 举个例子：
 * - 英雄"提莫"可能有一个修正器：modifierName="伤害倍率", modifierValue=0.85（伤害降低 15%）
 * - 英雄"盖伦"可能有一个修正器：modifierName="生命值加成", modifierValue=1.10（生命值增加 10%）
 *
 * 一个英雄可以有多个修正器，通过 heroId 关联到 Hero 表。
 *
 * 数据流向：
 * 数据库 tb_hero_modifier 表 → MyBatis-Plus 映射为 HeroModifier 对象
 * → HeroService 查询并组装到 HeroDetailVO.modifiers 中 → 返回给前端
 *
 * 关联类：
 * - HeroService：查询英雄详情时会同时查询修正器
 * - HeroModifierMapper：修正器的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_hero_modifier") // 对应数据库表 tb_hero_modifier
public class HeroModifier {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联英雄 ID —— 外键，关联 tb_hero 表的 id
     * 通过这个字段，我们知道这个修正器属于哪个英雄
     */
    private Long heroId;

    /**
     * 修正器名称 —— 描述修正的是什么属性
     * 如 "伤害倍率"、"生命值加成"、"技能急速加成" 等
     */
    private String modifierName;

    /**
     * 修正值 —— 修正的具体数值
     * - 大于 1 表示增强（如 1.10 = 增加 10%）
     * - 小于 1 表示削弱（如 0.85 = 降低 15%）
     * - 使用 BigDecimal 保证精度
     */
    private BigDecimal modifierValue;
}
