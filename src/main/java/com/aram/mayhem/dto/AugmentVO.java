package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 符文详情视图对象 —— 继承 AugmentListVO，扩展详情页需要的字段
 *
 * 和 HeroDetailVO 的设计思路一样：列表页用父类，详情页用子类。
 * 详情页需要展示符文的完整描述、所有套装信息和陷阱标记。
 *
 * 数据流向：
 * AugmentService 从数据库查出 Augment 实体 → 映射为 AugmentVO → 返回给前端
 *
 * 关联类：
 * - AugmentListVO：父类，包含列表页的基础字段
 * - Augment：数据库实体类
 */
@Data
@EqualsAndHashCode(callSuper = true) // equals/hashCode 方法包含父类字段
public class AugmentVO extends AugmentListVO {

    /** 符文描述/效果说明 —— 详细描述符文的具体效果 */
    private String description;

    /** 第二套装名称 —— 一个符文可以属于多个套装，可为 null */
    private String synergySet2;

    /** 第三套装名称 —— 同上，可为 null */
    private String synergySet3;

    /** 是否为陷阱符文 —— 数据表现差，前端用警告标记提醒用户 */
    private Boolean isTrap;
}
