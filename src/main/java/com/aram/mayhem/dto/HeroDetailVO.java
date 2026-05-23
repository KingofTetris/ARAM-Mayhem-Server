package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 英雄详情视图对象 —— 继承 HeroListVO，扩展详情页需要的额外字段
 *
 * 继承关系说明：
 * HeroListVO（列表页基础信息）→ HeroDetailVO（详情页完整信息）
 * 列表页只需要少量字段，详情页需要全部字段。
 * 通过继承避免重复定义列表页已有的字段。
 *
 * @EqualsAndHashCode(callSuper = true) 表示 equals 和 hashCode 方法也会考虑父类的字段，
 * 确保两个 HeroDetailVO 对象在所有字段（包括继承的字段）都相同时才判定为相等。
 *
 * 数据流向：
 * HeroService 从数据库查出 Hero 实体 + 关联数据 → 组装为 HeroDetailVO → 返回给前端
 *
 * 关联类：
 * - HeroListVO：父类，包含列表页的基础字段
 * - Hero：数据库实体类
 * - AugmentBriefVO：推荐符文的简要信息
 */
@Data
@EqualsAndHashCode(callSuper = true) // equals/hashCode 方法包含父类字段
public class HeroDetailVO extends HeroListVO {

    /** 英雄描述/简介 —— 一段文字介绍英雄的背景和特点 */
    private String description;

    /** 技能列表 —— 英雄的 Q/W/E/R/被动技能信息 */
    private List<SkillInfo> skills;

    /** 克制技巧列表 —— 对抗这个英雄的建议，如 ["避免近身对拼","利用远程消耗"] */
    private List<String> counterTips;

    /** 协同英雄列表 —— 与这个英雄配合好的英雄名，如 ["阿狸","娑娜"] */
    private List<String> synergies;

    /** 场均击杀数 —— 如 5.30，帮助判断英雄的击杀能力 */
    private BigDecimal avgKills;

    /** 场均死亡数 —— 如 6.10，帮助判断英雄的生存能力 */
    private BigDecimal avgDeaths;

    /** 场均助攻数 —— 如 8.40，帮助判断英雄的团队贡献 */
    private BigDecimal avgAssists;

    /** 推荐出装 —— JSON 格式的装备推荐方案 */
    private String recommendedBuild;

    /** 推荐强化符文列表 —— 包含符文的简要信息（名称、品质、图标） */
    private List<AugmentBriefVO> recommendedAugments;

    /**
     * 技能信息内部类 —— 描述英雄的一个技能
     *
     * 每个英雄有 5 个技能：被动 + Q + W + E + R
     * 这个类只保留前端展示需要的字段，不包含伤害数值等游戏内部数据。
     */
    @Data
    public static class SkillInfo {
        /** 技能按键 —— Q/W/E/R/被动，前端用这个显示技能图标 */
        private String key;
        /** 技能名称 —— 如 暗裔利刃 */
        private String name;
        /** 技能描述 —— 技能效果的详细说明 */
        private String description;
    }
}
