package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARAM 英雄统计数据 DTO
 *
 * 用途：AramDataCollector 采集结果 → DataAggregatorService 聚合输入
 * 数据来源：U.GG ARAM 页面（Jsoup HTML 解析）
 * 字段说明：所有百分比字段均为百分比格式（如 52.30 表示 52.30%）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AramHeroStatsDTO {

    /** 英雄英文名（如 Aatrox），用于与 RiotDataDragon 数据关联 */
    private String championName;

    /** ARAM 梯级评级（S+/S/A/B/C） */
    private String tier;

    /** ARAM 胜率（百分比格式，如 52.30） */
    private BigDecimal winRate;

    /** ARAM 选取率（百分比格式，如 15.30） */
    private BigDecimal pickRate;

    /** 场均击杀数 */
    private BigDecimal avgKills;

    /** 场均死亡数 */
    private BigDecimal avgDeaths;

    /** 场均助攻数 */
    private BigDecimal avgAssists;

    /** 数据来源标识（如 "u.gg"） */
    private String source;
}
