package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 套装进度响应对象
 *
 * 数据流向：AugmentService → AugmentController → 前端符文推荐页
 * 用途：展示当前已选符文的套装激活进度
 */
@Data
public class SynergyProgressResponse {

    /** 套装名称（如 精密、主宰） */
    private String synergyName;

    /** 当前已激活数量 */
    private int currentCount;

    /** 激活所需总数 */
    private int totalCount;

    /** 激活进度（0~1 之间） */
    private double progress;

    /** 激活状态（如 未激活/部分激活/已激活） */
    private String status;

    /** 该套装平均胜率 */
    private BigDecimal avgWinRate;
}