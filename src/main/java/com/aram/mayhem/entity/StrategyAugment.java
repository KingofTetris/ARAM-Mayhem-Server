package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 攻略符文关联实体类
 *
 * 对应表：tb_strategy_augment
 * 数据流向：MyBatis-Plus ↔ StrategyService → StrategyDetailVO.augments
 * 关联：StrategyService, StrategyAugmentMapper
 */
@Data
@TableName("tb_strategy_augment")
public class StrategyAugment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联攻略 ID */
    private Long strategyId;

    /** 关联符文 ID */
    private Long augmentId;
}
