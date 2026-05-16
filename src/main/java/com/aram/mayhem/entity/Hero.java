package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 英雄实体类
 *
 * 对应表：tb_hero
 * 数据流向：MyBatis-Plus ↔ Service ↔ Controller → DTO/VO
 * 关联：HeroController, HeroService, HeroMapper
 */
@Data
@TableName(value = "tb_hero", autoResultMap = true)
public class Hero {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Riot 官方英雄ID */
    private Integer riotId;

    /** 英雄英文名（如 Aatrox） */
    private String nameEn;

    /** 英雄中文名（如 亚托克斯） */
    private String nameZh;

    /** 英雄称号（如 暗裔剑魔） */
    private String title;

    /** 英雄定位（如 战士、法师、坦克等） */
    private String role;

    /** 英雄头像/图标 URL */
    private String imageUrl;

    /** 梯级评级（S+/S/A/B/C） */
    private String tier;

    /** ARAM 胜率（0~1 之间，如 0.532 表示 53.2%） */
    private BigDecimal winRate;

    /** ARAM 选取率（0~1 之间） */
    private BigDecimal pickRate;

    /** 数据置信度等级（如 high/medium/low） */
    private String confidenceLevel;

    /** 英雄描述/简介 */
    private String description;

    /** 技能列表（JSON 格式存储，含 key/name/description） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SkillData> skills;

    /** 克制技巧列表（JSON 格式存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> counterTips;

    /** 协同英雄列表（JSON 格式存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> synergies;

    /** 场均击杀数 */
    private BigDecimal avgKills;

    /** 场均死亡数 */
    private BigDecimal avgDeaths;

    /** 场均助攻数 */
    private BigDecimal avgAssists;

    /** 推荐出装（JSON 格式存储） */
    private String recommendedBuild;

    /** 推荐强化符文ID列表（JSON 格式存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> recommendedAugmentIds;

    /** 数据版本号（如 14.8） */
    private String version;

    /** 是否为版本陷阱英雄（被大幅削弱，慎用） */
    private Boolean isVersionTrap;

    /** 版本陷阱标记时间，取消标记时置 null */
    private LocalDateTime versionTrapSince;

    /** 数据更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 技能数据结构
     *
     * key: 技能按键（Q/W/E/R/被动）
     * name: 技能名称
     * description: 技能描述
     */
    @Data
    public static class SkillData {
        private String key;
        private String name;
        private String description;
    }
}
