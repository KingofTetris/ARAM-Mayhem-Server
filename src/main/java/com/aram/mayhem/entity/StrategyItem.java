package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tb_strategy_item")
public class StrategyItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long strategyId;

    private String itemName;

    private String itemCategory;

    private Integer sortOrder;
}
