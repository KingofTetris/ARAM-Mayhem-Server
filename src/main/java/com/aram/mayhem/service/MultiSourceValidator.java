package com.aram.mayhem.service;

import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;

import java.util.List;

/**
 * 多源数据交叉验证服务接口
 *
 * 功能：对比不同数据源的英雄/符文统计数据，评估数据置信度
 * 验证规则：
 *   - 双源数据差值 ≤ 2% → HIGH
 *   - 双源数据差值 2%~5% → MEDIUM
 *   - 双源数据差值 > 5% 或仅单源 → LOW
 * 关联：DataSyncScheduler（同步后调用验证）
 */
public interface MultiSourceValidator {

    /**
     * 验证英雄统计数据
     *
     * @param heroes 聚合后的英雄列表
     * @return 验证结果列表
     */
    List<ValidationResult> validateHeroStats(List<Hero> heroes);

    /**
     * 验证符文统计数据
     *
     * @param augments 聚合后的符文列表
     * @return 验证结果列表
     */
    List<ValidationResult> validateAugmentStats(List<Augment> augments);
}
