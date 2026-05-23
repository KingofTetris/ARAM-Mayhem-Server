package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.service.MultiSourceValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 多源数据验证器实现类 —— 数据质量的"质检员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 MultiSourceValidator 接口，负责验证聚合后的数据质量。
 * 它会检查每个英雄和符文的数据是否合理，并给出置信度评级。
 *
 * 为什么要验证数据质量？
 * - 外部数据源可能返回异常值（如胜率200%）
 * - 部分英雄可能缺少统计数据（使用默认值填充）
 * - 需要标记低质量数据，提醒用户谨慎参考
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、置信度评级体系
 * ═══════════════════════════════════════════════════════════════════
 *
 * 置信度      | 含义                    | 判定条件
 * ──────────────────────────────────────────────────────────────────────
 * HIGH        | 数据可靠，可直接使用      | 所有字段在合理范围内
 * MEDIUM      | 数据基本可用，略有偏差    | 偏差在2~5之间
 * LOW         | 数据不可靠，仅供参考      | 偏差>5 或有关键字段缺失
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、验证规则说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 验证项          | 合理范围           | 说明
 * ──────────────────────────────────────────────────────────────────────
 * 英雄胜率        | 40%~60%            | ARAM模式胜率通常在此范围
 * 英雄选取率      | 0%~30%             | 极少数英雄选取率超过30%
 * 符文胜率        | 40%~60%            | 同英雄
 * 符文选取率      | 0%~30%             | 同英雄
 * 符文平均排名    | 1~8                | 8人游戏，排名1~8
 * 段位            | 非null/空          | 必须有段位评级
 * 品质            | 非null/空          | 必须有品质（符文）
 * 置信度标记      | 非low              | 如果标记为low，说明数据来自默认值
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                              | 是否事务
 * ------------------------|----------------------------------|----------
 * validateHeroStats()     | 批量验证英雄数据                  | 否
 * validateAugmentStats()  | 批量验证符文数据                  | 否
 * validateSingleHero()    | 验证单个英雄数据（私有）          | 否
 * validateSingleAugment() | 验证单个符文数据（私有）          | 否
 * checkRange()            | 检查数值范围（私有）              | 否
 * checkNullField()        | 检查空字段（私有）                | 否
 * checkConfidenceLevel()  | 检查置信度标记（私有）            | 否
 * determineConfidence()   | 根据差异列表确定置信度（私有）    | 否
 */
@Slf4j
@Service
public class MultiSourceValidatorImpl implements MultiSourceValidator {

    /**
     * 高偏差阈值 —— 偏差超过此值可能为 MEDIUM 置信度
     *
     * 如果字段偏差超过2（如胜率偏差2%），置信度可能降为 MEDIUM
     */
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("2.0");

    /**
     * 中偏差阈值 —— 偏差超过此值为 LOW 置信度
     *
     * 如果字段偏差超过5（如胜率偏差5%），置信度降为 LOW
     */
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("5.0");

    /**
     * 批量验证英雄数据 —— 检查所有英雄的数据质量
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 遍历所有英雄，逐个验证数据质量，返回每个英雄的验证结果。
     * 最后统计各置信度级别的数量，便于整体评估数据质量。
     *
     * @param heroes 英雄实体列表
     * @return List<ValidationResult> 验证结果列表
     */
    @Override
    public List<ValidationResult> validateHeroStats(List<Hero> heroes) {
        log.info("[VALIDATE] hero stats validation started | count={}", heroes.size());

        List<ValidationResult> results = new ArrayList<>();
        for (Hero hero : heroes) {
            ValidationResult result = validateSingleHero(hero);
            results.add(result);
        }

        // ─── 统计各置信度数量 ───
        long highCount = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long mediumCount = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long lowCount = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();

        log.info("[VALIDATE] hero stats validation completed | HIGH={} | MEDIUM={} | LOW={}",
                highCount, mediumCount, lowCount);
        return results;
    }

    /**
     * 批量验证符文数据 —— 检查所有符文的数据质量
     *
     * 与 validateHeroStats() 逻辑相同，只是验证的是符文数据。
     *
     * @param augments 符文实体列表
     * @return List<ValidationResult> 验证结果列表
     */
    @Override
    public List<ValidationResult> validateAugmentStats(List<Augment> augments) {
        log.info("[VALIDATE] augment stats validation started | count={}", augments.size());

        List<ValidationResult> results = new ArrayList<>();
        for (Augment augment : augments) {
            ValidationResult result = validateSingleAugment(augment);
            results.add(result);
        }

        long highCount = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long mediumCount = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long lowCount = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();

        log.info("[VALIDATE] augment stats validation completed | HIGH={} | MEDIUM={} | LOW={}",
                highCount, mediumCount, lowCount);
        return results;
    }

    /**
     * 验证单个英雄数据 —— 检查英雄各字段是否合理
     *
     * ══════════════════════════════════════════════════════════════
     * 验证项
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 胜率是否在 40%~60% 范围内
     * 2. 选取率是否在 0%~30% 范围内
     * 3. 段位是否为空
     * 4. 置信度标记是否为 low
     *
     * @param hero 英雄实体
     * @return ValidationResult 验证结果
     */
    private ValidationResult validateSingleHero(Hero hero) {
        List<ValidationResult.FieldDifference> differences = new ArrayList<>();

        // 检查胜率是否在合理范围（ARAM模式通常40%~60%）
        checkRange(hero.getWinRate(), new BigDecimal("40"), new BigDecimal("60"), "winRate", differences);
        // 检查选取率是否在合理范围
        checkRange(hero.getPickRate(), BigDecimal.ZERO, new BigDecimal("30"), "pickRate", differences);
        // 检查段位是否为空
        checkNullField(hero.getTier(), "tier", differences);
        // 检查置信度标记是否为 low
        checkConfidenceLevel(hero.getConfidenceLevel(), differences);

        // 根据差异列表确定置信度
        String confidence = determineConfidence(differences);
        // LOW 置信度视为未通过验证
        boolean passed = !"LOW".equals(confidence);

        if ("LOW".equals(confidence)) {
            log.debug("[VALIDATE] hero={} | confidence=LOW | differences={}", hero.getNameEn(), differences.size());
        }

        return ValidationResult.builder()
                .targetName(hero.getNameEn())
                .targetType("hero")
                .confidenceLevel(confidence)
                .passed(passed)
                .differences(differences)
                .build();
    }

    /**
     * 验证单个符文数据 —— 检查符文各字段是否合理
     *
     * ══════════════════════════════════════════════════════════════
     * 验证项
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 胜率是否在 40%~60% 范围内
     * 2. 选取率是否在 0%~30% 范围内
     * 3. 平均排名是否在 1~8 范围内
     * 4. 段位是否为空
     * 5. 品质是否为空
     *
     * @param augment 符文实体
     * @return ValidationResult 验证结果
     */
    private ValidationResult validateSingleAugment(Augment augment) {
        List<ValidationResult.FieldDifference> differences = new ArrayList<>();

        // 检查胜率
        checkRange(augment.getWinRate(), new BigDecimal("40"), new BigDecimal("60"), "winRate", differences);
        // 检查选取率
        checkRange(augment.getPickRate(), BigDecimal.ZERO, new BigDecimal("30"), "pickRate", differences);
        // 检查平均排名（仅当值不为null时检查）
        if (augment.getAvgPlacement() != null) {
            checkRange(augment.getAvgPlacement(), BigDecimal.ONE, new BigDecimal("8"), "avgPlacement", differences);
        }
        // 检查段位
        checkNullField(augment.getTier(), "tier", differences);
        // 检查品质
        checkNullField(augment.getQuality(), "quality", differences);

        String confidence = determineConfidence(differences);
        boolean passed = !"LOW".equals(confidence);

        return ValidationResult.builder()
                .targetName(augment.getNameEn())
                .targetType("augment")
                .confidenceLevel(confidence)
                .passed(passed)
                .differences(differences)
                .build();
    }

    /**
     * 检查数值范围 —— 验证字段值是否在预期范围内
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 检查数值是否在 [expectedMin, expectedMax] 范围内。
     * 如果超出范围，计算偏差值（delta）并记录到差异列表中。
     *
     * @param value       要检查的值
     * @param expectedMin 预期最小值
     * @param expectedMax 预期最大值
     * @param fieldName   字段名称
     * @param differences 差异列表（输出参数）
     *
     * ══════════════════════════════════════════════════════════════
     * 偏差计算方式
     * ══════════════════════════════════════════════════════════════
     *
     * - 如果 value < expectedMin → delta = expectedMin - value
     * - 如果 value > expectedMax → delta = value - expectedMax
     * - 如果在范围内 → 不记录差异
     *
     * 例如：胜率=35%，预期范围[40%, 60%]
     * → delta = 40 - 35 = 5（偏差5%）
     */
    private void checkRange(BigDecimal value, BigDecimal expectedMin, BigDecimal expectedMax,
                            String fieldName, List<ValidationResult.FieldDifference> differences) {
        if (value == null) {
            // 值为null，记录为差异
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value("null")
                    .description(fieldName + " is null")
                    .build());
            return;
        }

        // 计算偏差值
        BigDecimal delta = BigDecimal.ZERO;
        if (value.compareTo(expectedMin) < 0) {
            // 值低于最小值
            delta = expectedMin.subtract(value);
        } else if (value.compareTo(expectedMax) > 0) {
            // 值高于最大值
            delta = value.subtract(expectedMax);
        }

        // 如果有偏差，记录差异
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value(value.toPlainString())
                    .delta(delta.toPlainString())
                    .description(fieldName + "=" + value + " outside expected range [" + expectedMin + ", " + expectedMax + "]")
                    .build());
        }
    }

    /**
     * 检查空字段 —— 验证关键字段是否为空
     *
     * @param value     字段值
     * @param fieldName 字段名称
     * @param differences 差异列表（输出参数）
     */
    private void checkNullField(Object value, String fieldName, List<ValidationResult.FieldDifference> differences) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value(value == null ? "null" : "blank")
                    .description(fieldName + " is null or blank")
                    .build());
        }
    }

    /**
     * 检查置信度标记 —— 验证数据的置信度标记
     *
     * 如果置信度标记为 "low"，说明数据来自默认值而非实际统计，
     * 需要记录为差异，提醒用户谨慎参考。
     *
     * @param confidenceLevel 置信度标记
     * @param differences 差异列表（输出参数）
     */
    private void checkConfidenceLevel(String confidenceLevel, List<ValidationResult.FieldDifference> differences) {
        if (confidenceLevel == null || confidenceLevel.isBlank()) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName("confidenceLevel")
                    .source1Value("null")
                    .description("confidenceLevel is null or blank")
                    .build());
            return;
        }
        if ("low".equalsIgnoreCase(confidenceLevel)) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName("confidenceLevel")
                    .source1Value(confidenceLevel)
                    .description("confidenceLevel is LOW - data from single source or default values")
                    .build());
        }
    }

    /**
     * 确定置信度 —— 根据差异列表确定数据的置信度级别
     *
     * ══════════════════════════════════════════════════════════════
     * 判定逻辑
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 无差异 → HIGH（数据完全合理）
     * 2. 有差异且最大偏差 > 5 → LOW（数据不可靠）
     * 3. 有差异且 delta 为 null（字段缺失）→ LOW
     * 4. 有差异且最大偏差 > 2 → MEDIUM（数据基本可用）
     * 5. 其他 → MEDIUM
     *
     * 判定优先级：LOW > MEDIUM > HIGH
     * 只要有一个字段严重偏差，整体就降为 LOW
     *
     * @param differences 差异列表
     * @return 置信度级别："HIGH"、"MEDIUM" 或 "LOW"
     */
    private String determineConfidence(List<ValidationResult.FieldDifference> differences) {
        // 无差异 → HIGH
        if (differences.isEmpty()) {
            return "HIGH";
        }

        // ─── 第一轮：检查是否有严重偏差（delta > 5 或 delta 为 null） ───
        for (ValidationResult.FieldDifference diff : differences) {
            if (diff.getDelta() != null) {
                BigDecimal delta = new BigDecimal(diff.getDelta());
                // 偏差超过5 → LOW
                if (delta.compareTo(MEDIUM_THRESHOLD) > 0) {
                    return "LOW";
                }
            }
            // delta 为 null（字段缺失）→ LOW
            if (diff.getDelta() == null) {
                return "LOW";
            }
        }

        // ─── 第二轮：检查是否有中等偏差（delta > 2） ───
        for (ValidationResult.FieldDifference diff : differences) {
            if (diff.getDelta() != null) {
                BigDecimal delta = new BigDecimal(diff.getDelta());
                // 偏差超过2 → MEDIUM
                if (delta.compareTo(HIGH_THRESHOLD) > 0) {
                    return "MEDIUM";
                }
            }
        }

        // 有差异但偏差很小 → MEDIUM
        return "MEDIUM";
    }
}
