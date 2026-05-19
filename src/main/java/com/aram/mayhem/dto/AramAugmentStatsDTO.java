package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARAM 强化符文统计数据 DTO
 *
 * 用途：AramDataCollector 采集结果 → DataAggregatorService 聚合输入
 * 数据来源：U.GG / ARAMMayhem 页面（Jsoup HTML 解析）
 * 字段说明：所有百分比字段均为百分比格式（如 52.30 表示 52.30%）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AramAugmentStatsDTO {

    /** 符文名称 */
    private String augmentName;

    /** 符文品质（如 银色/金色/棱彩） */
    private String quality;

    /** ARAM 胜率（百分比格式，如 52.30） */
    private BigDecimal winRate;

    /** ARAM 选取率（百分比格式，如 15.30） */
    private BigDecimal pickRate;

    /** 平均排名（1~8 之间，越小越好） */
    private BigDecimal avgPlacement;

    /** ARAM 梯级评级（S+/S/A/B/C） */
    private String tier;

    /** 套装名称（如 精密） */
    private String synergySet;

    /** 数据来源标识（如 "u.gg"） */
    private String source;
}
