package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 符文详情视图对象
 *
 * 继承 AugmentListVO，扩展详情字段
 * 数据流向：AugmentService → AugmentController → 前端符文详情页
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AugmentVO extends AugmentListVO {

    /** 符文描述/效果说明 */
    private String description;

    /** 第二套装名称 */
    private String synergySet2;

    /** 第三套装名称 */
    private String synergySet3;

    /** 是否为陷阱符文（数据表现差，不推荐） */
    private Boolean isTrap;
}
