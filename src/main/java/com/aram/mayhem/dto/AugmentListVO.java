package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 符文列表视图对象 —— 用于符文列表页展示
 *
 * 和 HeroListVO 类似，这个类只包含列表页需要展示的字段。
 * 符文详情页使用 AugmentVO（继承此类）展示更多信息。
 *
 * 数据流向：
 * AugmentService 从数据库查出 Augment 实体 → 映射为 AugmentListVO → 返回给前端
 *
 * 关联类：
 * - Augment：数据库实体类
 * - AugmentVO：符文详情视图对象（继承此类）
 */
@Data
public class AugmentListVO {

    /** 符文 ID —— 唯一标识，点击时用这个跳转详情页 */
    private Long id;

    /** 符文中文名 —— 如 "狂战之刃"，列表页显示用 */
    private String nameZh;

    /** 符文英文名 —— 如 "Warlord's Blade"，用于数据匹配 */
    private String nameEn;

    /** 符文品质 —— 银色/金色/棱彩，前端用不同颜色展示 */
    private String quality;

    /** 第一套装名称 —— 如 "精密"，前端展示套装标签 */
    private String synergySet;

    /** 符文图标 URL —— 列表页显示符文图标 */
    private String iconUrl;

    /** ARAM 胜率 —— 如 0.5230 表示 52.30% */
    private BigDecimal winRate;

    /** ARAM 选取率 —— 如 0.1530 表示 15.30% */
    private BigDecimal pickRate;

    /** 平均排名 —— 1~8 之间，越小越好 */
    private BigDecimal avgPlacement;

    /** 梯级评级 —— S+/S/A/B/C，前端用颜色区分 */
    private String tier;
}
