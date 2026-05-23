package com.aram.mayhem.dto;

import lombok.Data;

/**
 * 装备物品视图对象 —— 攻略详情页中展示推荐装备
 *
 * 这个类用于攻略详情页的"推荐装备"区域，
 * 显示装备的名称和图标，帮助玩家了解出装方案。
 *
 * 数据流向：
 * StrategyService 查询攻略关联的装备 → 组装为 ItemVO → 放入 StrategyDetailVO.items
 *
 * 关联类：
 * - StrategyDetailVO：攻略详情 VO，包含 items 字段
 */
@Data
public class ItemVO {

    /** 装备 ID —— 唯一标识 */
    private Long id;

    /** 装备中文名 —— 如 "黑色切割者" */
    private String nameZh;

    /** 装备英文名 —— 如 "Black Cleaver" */
    private String nameEn;

    /** 装备图标 URL —— 前端显示装备图标 */
    private String icon;
}
