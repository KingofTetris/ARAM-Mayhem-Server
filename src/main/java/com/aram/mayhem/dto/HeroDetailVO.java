package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 英雄详情视图对象
 *
 * 继承 HeroListVO，扩展详情字段
 * 数据流向：HeroService → HeroController → 前端英雄详情页
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HeroDetailVO extends HeroListVO {

    /** 英雄描述/简介 */
    private String description;

    /** 技能列表 */
    private List<SkillInfo> skills;

    /** 克制技巧列表 */
    private List<String> counterTips;

    /** 协同英雄列表 */
    private List<String> synergies;

    /** 场均击杀数 */
    private BigDecimal avgKills;

    /** 场均死亡数 */
    private BigDecimal avgDeaths;

    /** 场均助攻数 */
    private BigDecimal avgAssists;

    /** 推荐出装 */
    private String recommendedBuild;

    /**
     * 技能信息
     *
     * key: 技能按键（Q/W/E/R/被动）
     * name: 技能名称
     * description: 技能描述
     */
    @Data
    public static class SkillInfo {
        private String key;
        private String name;
        private String description;
    }
}
