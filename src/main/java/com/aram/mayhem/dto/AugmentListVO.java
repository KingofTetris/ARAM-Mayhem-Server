package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 符文列表视图对象
 *
 * 数据流向：AugmentService → AugmentController → 前端符文列表页
 * 用途：符文列表展示，包含基础统计信息
 */
@Data
public class AugmentListVO {

    private Long id;

    /** 符文中文名 */
    private String nameZh;

    /** 符文英文名 */
    private String nameEn;

    /** 符文品质（银色/金色/棱彩） */
    private String quality;

    /** 第一套装名称 */
    private String synergySet;

    /** 符文图标 URL */
    private String iconUrl;

    /** ARAM 胜率（0~1 之间） */
    private BigDecimal winRate;

    /** ARAM 选取率（0~1 之间） */
    private BigDecimal pickRate;

    /** 平均排名（1~8 之间） */
    private BigDecimal avgPlacement;

    /** 梯级评级（S+/S/A/B/C） */
    private String tier;
}
