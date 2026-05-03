package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tb_strategy_augment")
public class StrategyAugment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long strategyId;

    private Long augmentId;
}
