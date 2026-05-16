package com.aram.mayhem.dto;

import lombok.Data;

/**
 * 装备物品视图对象
 *
 * 数据流向：StrategyService → StrategyDetailVO.items → 前端攻略详情页
 */
@Data
public class ItemVO {
    private Long id;
    /** 物品中文名 */
    private String nameZh;
    /** 物品英文名 */
    private String nameEn;
    /** 物品图标 URL */
    private String icon;
}