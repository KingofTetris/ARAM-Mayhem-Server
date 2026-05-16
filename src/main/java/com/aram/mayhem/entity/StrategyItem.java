package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 攻略出装物品关联实体类
 *
 * 对应表：tb_strategy_item
 * 数据流向：MyBatis-Plus ↔ StrategyService → StrategyDetailVO.items
 * 关联：StrategyService, StrategyItemMapper
 */
@Data
@TableName("tb_strategy_item")
public class StrategyItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联攻略 ID */
    private Long strategyId;

    /** 物品名称 */
    private String itemName;

    /** 物品分类（如 起始装备/核心装备/可选装备） */
    private String itemCategory;

    /** 排序序号（越小越靠前） */
    private Integer sortOrder;
}
