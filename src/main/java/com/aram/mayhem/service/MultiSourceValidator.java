package com.aram.mayhem.service;

import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;

import java.util.List;

/**
 * 多源数据交叉验证服务接口 —— 数据质量的"质检员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口负责对聚合后的数据进行质量检查，评估数据的可信程度。
 * 就像工厂的产品质检部门，检查产品是否合格。
 *
 * 打个比方：
 * - 数据聚合 = 生产产品（把原材料加工成成品）
 * - 数据验证 = 质量检查（检查成品是否合格）
 * - 置信度 = 质量等级（HIGH=优质品、MEDIUM=合格品、LOW=次品）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、置信度评估规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * 置信度分为三个等级：
 *
 * 【HIGH（高置信度）】
 * - 所有字段都在合理范围内，没有异常
 * - 数据来自多个数据源且差异很小
 * - 可以放心使用
 *
 * 【MEDIUM（中等置信度）】
 * - 部分字段有轻微偏差（差值在2%~5%之间）
 * - 数据基本可用，但建议关注
 *
 * 【LOW（低置信度）】
 * - 字段偏差较大（差值超过5%）
 * - 关键字段为null或空
 * - 数据来自单一数据源或使用了默认值
 * - 建议谨慎使用，可能需要人工审核
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、验证检查项
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【英雄数据验证】
 * - winRate 是否在 40%~60% 的合理范围内
 * - pickRate 是否在 0%~30% 的合理范围内
 * - tier 是否为空
 * - confidenceLevel 是否为"low"
 *
 * 【符文数据验证】
 * - winRate 是否在 40%~60% 的合理范围内
 * - pickRate 是否在 0%~30% 的合理范围内
 * - avgPlacement 是否在 1~8 的合理范围内
 * - tier 是否为空
 * - quality 是否为空
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、实现类
 * ═══════════════════════════════════════════════════════════════════
 *
 * @see com.aram.mayhem.service.impl.MultiSourceValidatorImpl 多源数据验证服务实现类
 */
public interface MultiSourceValidator {

    /**
     * 验证英雄统计数据 —— 检查聚合后的英雄数据质量
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 对聚合后的英雄列表逐个进行数据质量检查，返回每个英雄的验证结果。
     *
     * @param heroes 聚合后的英雄列表（来自 DataAggregatorService）
     * @return List<ValidationResult> 验证结果列表，每个元素包含：
     *   - targetName → 英雄英文名
     *   - targetType → "hero"
     *   - confidenceLevel → 置信度（HIGH/MEDIUM/LOW）
     *   - passed → 是否通过验证（LOW=false，其他=true）
     *   - differences → 异常字段列表（描述哪些字段有问题）
     *
     * ═══════════════════════════════════════════════════════════════
     * 调用示例
     * ═══════════════════════════════════════════════════════════════
     *
     * // 验证聚合后的英雄数据
     * List<ValidationResult> results = validator.validateHeroStats(heroes);
     * // 统计各置信度的数量
     * long highCount = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
     * long lowCount = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();
     * System.out.println("高置信度: " + highCount + ", 低置信度: " + lowCount);
     */
    List<ValidationResult> validateHeroStats(List<Hero> heroes);

    /**
     * 验证符文统计数据 —— 检查聚合后的符文数据质量
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 对聚合后的符文列表逐个进行数据质量检查，返回每个符文的验证结果。
     *
     * @param augments 聚合后的符文列表（来自 DataAggregatorService）
     * @return List<ValidationResult> 验证结果列表
     */
    List<ValidationResult> validateAugmentStats(List<Augment> augments);
}
