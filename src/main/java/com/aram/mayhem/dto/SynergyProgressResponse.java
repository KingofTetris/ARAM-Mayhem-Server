package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 套装进度响应对象 —— 展示当前已选符文的套装激活进度
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM Mayhem 模式中，符文分为不同的"套装"（Synergy Set），
 * 当玩家选择同一套装的符文达到一定数量时，会激活额外的套装效果。
 *
 * 这个类用于展示每个套装的激活进度，让玩家知道：
 * - "精密"套装还需要几个符文就能激活？
 * - "主宰"套装已经激活了吗？
 *
 * 就像集齐卡片兑换奖品：
 * - currentCount = 你已经集了几张卡
 * - totalCount = 需要集齐几张卡
 * - progress = 完成进度百分比
 * - status = 当前状态（还没开始/进行中/已完成）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、套装机制说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM Mayhem 中的套装示例：
 * - 精密（Precision）：3个精密符文激活，提供攻击速度加成
 * - 主宰（Domination）：3个主宰符文激活，提供爆发伤害加成
 * - 巫术（Sorcery）：3个巫术符文激活，提供法术强度加成
 *
 * 激活条件：同一套装的符文数量 ≥ totalCount
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentService.getSynergyProgress(heroId, selectedAugmentIds)
 *     ↓ 计算每个套装的激活进度
 * 生成 List<SynergyProgressResponse>
 *     ↓
 * AugmentController → HTTP 响应
 *     ↓
 * 前端 SynergyProgressSection → 展示进度条和套装名称
 */
@Data
public class SynergyProgressResponse {

    /**
     * 套装名称 —— 符文所属的协同效果组名称
     *
     * 示例："精密"、"主宰"、"巫术"
     * 前端直接显示此名称作为套装标题
     */
    private String synergyName;

    /**
     * 当前已激活数量 —— 已选符文中属于该套装的符文数量
     *
     * 示例：currentCount=2 表示已选了2个"精密"套装的符文
     *
     * 计算方式：遍历已选符文列表，统计属于该套装的符文数量
     */
    private int currentCount;

    /**
     * 激活所需总数 —— 激活该套装效果需要的最少符文数量
     *
     * 示例：totalCount=3 表示需要3个同套装符文才能激活
     *
     * 通常为3，但不同套装可能有不同的激活门槛
     */
    private int totalCount;

    /**
     * 激活进度 —— 当前进度占所需总数的比例
     *
     * 范围：0.0~1.0
     * 计算公式：progress = currentCount / totalCount
     *
     * 示例：
     * - currentCount=0, totalCount=3 → progress=0.0（0%）
     * - currentCount=1, totalCount=3 → progress=0.333（33.3%）
     * - currentCount=2, totalCount=3 → progress=0.667（66.7%）
     * - currentCount=3, totalCount=3 → progress=1.0（100%）
     *
     * 前端使用此值设置 ProgressBar 的进度
     */
    private double progress;

    /**
     * 激活状态 —— 套装当前的激活状态描述
     *
     * 取值范围：
     * - "未激活"：currentCount=0，还没选该套装的符文
     * - "部分激活"：0 < currentCount < totalCount，选了但还不够
     * - "已激活"：currentCount ≥ totalCount，套装效果已触发
     *
     * 前端根据状态显示不同的样式：
     * - 未激活：灰色文字
     * - 部分激活：黄色文字 + 进度条
     * - 已激活：绿色文字 + 闪烁动画
     */
    private String status;

    /**
     * 该套装平均胜率 —— 使用该套装符文的平均胜率
     *
     * 百分比格式，如 52.30 表示 52.30%
     * 用于帮助玩家判断是否值得追求该套装
     *
     * 示例：
     * - avgWinRate=54.5%：该套装胜率很高，值得追求
     * - avgWinRate=48.0%：该套装胜率较低，不如选其他符文
     */
    private BigDecimal avgWinRate;
}