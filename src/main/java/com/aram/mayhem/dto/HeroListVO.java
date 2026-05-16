package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 英雄列表视图对象
 *
 * 数据流向：HeroService → HeroController → 前端
 * 用途：英雄列表页展示，包含基础统计信息
 */
@Data
public class HeroListVO {

    private Long id;

    /** 英雄英文名 */
    private String nameEn;

    /** 英雄中文名 */
    private String nameZh;

    /** 英雄称号 */
    private String title;

    /** 英雄定位 */
    private String role;

    /** 梯级评级（S+/S/A/B/C） */
    private String tier;

    /** ARAM 胜率（0~1 之间） */
    private BigDecimal winRate;

    /** ARAM 选取率（0~1 之间） */
    private BigDecimal pickRate;

    /** 英雄头像 URL */
    private String imageUrl;

    /** 是否为版本陷阱英雄 */
    private Boolean isVersionTrap;
}
