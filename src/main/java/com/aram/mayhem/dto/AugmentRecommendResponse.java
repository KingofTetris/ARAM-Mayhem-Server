package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 符文推荐响应对象
 *
 * 数据流向：AugmentService → AugmentController → 前端符文推荐页
 * 用途：基于已选符文和英雄，返回推荐符文及评分
 */
@Data
public class AugmentRecommendResponse {

    private Long id;

    /** 符文中文名 */
    private String nameZh;

    /** 符文英文名 */
    private String nameEn;

    /** 符文品质 */
    private String quality;

    /** 套装名称 */
    private String synergySet;

    /** 符文图标 URL */
    private String iconUrl;

    /** ARAM 胜率 */
    private BigDecimal winRate;

    /** ARAM 选取率 */
    private BigDecimal pickRate;

    /** 平均排名 */
    private BigDecimal avgPlacement;

    /** 梯级评级 */
    private String tier;

    /** 是否为陷阱符文 */
    private Boolean isTrap;

    /** 推荐评分（0~100） */
    private double score;

    /** 推荐理由 */
    private String recommendationReason;
}