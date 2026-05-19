package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据验证结果 DTO
 *
 * 用途：MultiSourceValidator 验证结果 → DataSyncScheduler 日志记录
 * 置信度等级：
 *   HIGH   - 双源数据一致（差值 ≤ 2%）
 *   MEDIUM - 双源数据轻微偏差（2% < 差值 ≤ 5%）
 *   LOW    - 双源数据严重偏差（差值 > 5%）或仅有单源数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /** 验证目标标识（如英雄名/符文名） */
    private String targetName;

    /** 验证目标类型（hero/augment） */
    private String targetType;

    /** 置信度等级（HIGH/MEDIUM/LOW） */
    private String confidenceLevel;

    /** 是否通过验证（HIGH/MEDIUM 为通过，LOW 为未通过） */
    private boolean passed;

    /** 差异详情列表 */
    private List<FieldDifference> differences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDifference {

        /** 字段名 */
        private String fieldName;

        /** 源1的值 */
        private String source1Value;

        /** 源2的值（可为 null，表示单源数据） */
        private String source2Value;

        /** 差异值（绝对值） */
        private String delta;

        /** 差异描述 */
        private String description;
    }
}
