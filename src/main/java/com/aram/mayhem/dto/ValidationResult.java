package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据验证结果 DTO —— 记录多源数据交叉验证的结果
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本项目的数据来自多个外部源（Riot DataDragon + U.GG），
 * 同一个英雄/符文在不同数据源中可能有不同的数值。
 * 为了确保数据质量，需要对比多个数据源的数据，检查差异是否在可接受范围内。
 * ValidationResult 就是记录这个对比验证结果的对象。
 *
 * 就像质检员检查产品：
 * - targetName = 产品名称（如"亚索"）
 * - targetType = 产品类别（如"英雄"）
 * - confidenceLevel = 质检等级（高/中/低）
 * - passed = 是否合格
 * - differences = 具体的质量差异清单
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、置信度等级说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * confidenceLevel 有三个等级，表示数据源之间的一致程度：
 *
 * HIGH（高置信度）：
 *   - 双源数据一致（差值 ≤ 2%）
 *   - 示例：Riot 胜率 52.3%，U.GG 胜率 52.1%，差值 0.2% ≤ 2%
 *   - 处理：直接采用，无需人工干预
 *
 * MEDIUM（中置信度）：
 *   - 双源数据轻微偏差（2% < 差值 ≤ 5%）
 *   - 示例：Riot 胜率 52.3%，U.GG 胜率 48.0%，差值 4.3% 在 2%~5% 之间
 *   - 处理：采用平均值，记录差异供后续分析
 *
 * LOW（低置信度）：
 *   - 双源数据严重偏差（差值 > 5%）或仅有单源数据
 *   - 示例：Riot 胜率 52.3%，U.GG 胜率 40.0%，差值 12.3% > 5%
 *   - 处理：标记为验证未通过，需要人工审核
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * AramDataCollector（采集数据）
 *       ↓
 * DataAggregatorService（聚合多源数据）
 *       ↓
 * MultiSourceValidatorImpl（交叉验证）→ 生成 ValidationResult
 *       ↓
 * DataSyncScheduler（记录验证结果到 SyncResult）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 验证目标标识 —— 被验证的对象名称
     *
     * 对于英雄：存储英雄英文名（如 "Aatrox"）
     * 对于符文：存储符文名称（如 "Prowler's Claw"）
     * 用于在日志和 SyncResult 中标识具体是哪个数据项的验证结果
     */
    private String targetName;

    /**
     * 验证目标类型 —— 被验证的对象类别
     *
     * 取值范围：
     * - "hero"：英雄数据验证
     * - "augment"：符文数据验证
     *
     * 不同类型的验证规则和阈值可能不同：
     * - 英雄胜率差值阈值：5%
     * - 符文胜率差值阈值：8%（符文数据波动更大）
     */
    private String targetType;

    /**
     * 置信度等级 —— 数据源之间一致程度的评级
     *
     * 取值范围：HIGH / MEDIUM / LOW
     * 详见类级别注释"置信度等级说明"
     *
     * 判定规则：
     * - 差值 ≤ 2%  → HIGH
     * - 差值 ≤ 5%  → MEDIUM
     * - 差值 > 5% 或单源 → LOW
     */
    private String confidenceLevel;

    /**
     * 是否通过验证 —— 最终判定结果
     *
     * true：数据可信，可以写入数据库
     *   - confidenceLevel 为 HIGH 或 MEDIUM 时，passed=true
     *
     * false：数据不可信，需要人工审核
     *   - confidenceLevel 为 LOW 时，passed=false
     *
     * DataSyncScheduler 根据 passed 字段统计 validationPassed 和 validationFailed
     */
    private boolean passed;

    /**
     * 差异详情列表 —— 记录每个字段的具体差异
     *
     * 即使 passed=true（验证通过），differences 也可能不为空，
     * 因为 MEDIUM 级别的验证虽然通过，但仍然存在轻微差异。
     *
     * 示例：
     * - fieldName="winRate", source1Value="52.3", source2Value="52.1", delta="0.2"
     * - fieldName="pickRate", source1Value="15.3", source2Value="14.8", delta="0.5"
     */
    private List<FieldDifference> differences;

    /**
     * 字段差异详情 —— 记录单个字段在两个数据源之间的差异
     *
     * ═══════════════════════════════════════════════════════════════════
     * 使用场景
     * ═══════════════════════════════════════════════════════════════════
     *
     * 当同一个英雄/符文在两个数据源中的某个字段值不同时，
     * 就会生成一个 FieldDifference 对象，记录：
     * - 哪个字段不同（fieldName）
     * - 源1的值是多少（source1Value）
     * - 源2的值是多少（source2Value）
     * - 差了多少（delta）
     * - 差异说明（description）
     *
     * 示例：
     *   fieldName    = "winRate"
     *   source1Value = "52.30"  （Riot DataDragon）
     *   source2Value = "52.10"  （U.GG）
     *   delta        = "0.20"
     *   description  = "胜率差异 0.20%，在可接受范围内"
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDifference {

        /**
         * 字段名 —— 存在差异的数据字段名称
         *
         * 常见取值：
         * - "winRate"：胜率字段
         * - "pickRate"：选取率字段
         * - "avgKills"：场均击杀字段
         * - "avgDeaths"：场均死亡字段
         * - "avgAssists"：场均助攻字段
         */
        private String fieldName;

        /**
         * 源1的值 —— 第一个数据源（通常是 Riot DataDragon）的字段值
         *
         * 以字符串形式存储，因为不同字段的值类型不同：
         * - 胜率："52.30"
         * - 选取率："15.30"
         * - 击杀数："5.2"
         */
        private String source1Value;

        /**
         * 源2的值 —— 第二个数据源（通常是 U.GG）的字段值
         *
         * 可以为 null，表示该字段只有单源数据。
         * 单源数据时，confidenceLevel 会被设为 LOW。
         */
        private String source2Value;

        /**
         * 差异值（绝对值）—— 两个数据源之间的数值差
         *
         * 计算方式：delta = |source1Value - source2Value|
         * 示例：52.30 - 52.10 = 0.20
         *
         * 用于判断置信度等级：
         * - delta ≤ 2%  → HIGH
         * - delta ≤ 5%  → MEDIUM
         * - delta > 5%  → LOW
         */
        private String delta;

        /**
         * 差异描述 —— 人类可读的差异说明
         *
         * 示例：
         * - "胜率差异 0.20%，在可接受范围内"
         * - "选取率差异 6.50%，超出阈值，需人工审核"
         * - "仅单源数据，无法交叉验证"
         */
        private String description;
    }
}
