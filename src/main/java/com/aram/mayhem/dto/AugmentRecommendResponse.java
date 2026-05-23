package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 符文推荐响应对象 —— 后端返回给前端的推荐符文信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当前端请求符文推荐时（AugmentRecommendRequest），
 * 后端会返回一个推荐符文列表，每个推荐符文就是一个 AugmentRecommendResponse 对象。
 *
 * 与 AugmentVO 的区别：
 * - AugmentVO：符文的基础信息（用于符文列表展示）
 * - AugmentRecommendResponse：符文的推荐信息（包含推荐评分和理由）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、推荐评分算法
 * ═══════════════════════════════════════════════════════════════════
 *
 * score（推荐评分）的计算公式：
 * score = 胜率权重 × winRate + 选取率权重 × pickRate + 套装加成 + 陷阱惩罚
 *
 * 评分范围：0~100
 * - 90~100：强烈推荐
 * - 70~90：推荐
 * - 50~70：一般
 * - 0~50：不推荐
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentService.getRecommendations() → 生成 List<AugmentRecommendResponse>
 *     ↓
 * AugmentController → HTTP 响应
 *     ↓
 * 前端 AugmentRecommendFragment → 展示推荐列表
 */
@Data
public class AugmentRecommendResponse {

    /**
     * 符文ID —— 数据库主键，用于唯一标识一个符文
     *
     * 前端点击推荐符文时，使用此 ID 进行后续操作（如查看详情、添加到已选列表）
     */
    private Long id;

    /**
     * 符文中文名 —— 面向中文用户显示的名称
     *
     * 示例："掠夺者之爪"、"黑暗收割"
     * 前端推荐列表中直接显示此名称
     */
    private String nameZh;

    /**
     * 符文英文名 —— 面向英文用户显示的名称
     *
     * 示例："Prowler's Claw"、"Dark Harvest"
     * 用于 URL 路径、图片文件名等场景
     */
    private String nameEn;

    /**
     * 符文品质 —— 符文的稀有度等级
     *
     * 取值："银色" / "金色" / "棱彩"
     * 前端根据品质显示不同颜色的边框和背景
     */
    private String quality;

    /**
     * 套装名称 —— 该符文所属的协同效果组
     *
     * 示例："精密"、"主宰"、"巫术"
     * 前端展示套装进度时使用（与 SynergyProgressResponse 关联）
     */
    private String synergySet;

    /**
     * 符文图标 URL —— 符文图片的访问地址
     *
     * 示例："https://ddragon.leagueoflegends.com/cdn/img/augment/ProwlersClaw.png"
     * 前端使用 Glide 加载此 URL 显示符文图标
     */
    private String iconUrl;

    /**
     * ARAM 胜率 —— 使用该符文时的胜率
     *
     * 百分比格式，如 52.30 表示 52.30%
     * 是推荐评分的重要参考指标
     */
    private BigDecimal winRate;

    /**
     * ARAM 选取率 —— 该符文被选择的概率
     *
     * 百分比格式，如 15.30 表示 15.30%
     * 选取率过高可能意味着该符文过于强势或即将被削弱
     */
    private BigDecimal pickRate;

    /**
     * 平均排名 —— 使用该符文时的平均名次
     *
     * 范围 1~8，越小越好
     * 排名越靠前，说明该符文越能帮助队伍获胜
     */
    private BigDecimal avgPlacement;

    /**
     * 梯级评级 —— 综合胜率和选取率的等级评定
     *
     * 取值：S+ / S / A / B / C
     * 前端根据评级显示不同的标签颜色
     */
    private String tier;

    /**
     * 是否为陷阱符文 —— 标记该符文是否为版本陷阱
     *
     * true：该符文看起来很强，但实际胜率很低，不推荐选择
     * false：正常符文
     *
     * 前端显示逻辑：
     * - isTrap=true：显示红色警告标签"版本陷阱"
     * - isTrap=false：正常显示
     */
    private Boolean isTrap;

    /**
     * 推荐评分 —— 综合评估后的推荐分数
     *
     * 范围：0~100
     * 计算公式：score = 胜率权重 × winRate + 选取率权重 × pickRate + 套装加成 - 陷阱惩罚
     *
     * 前端根据评分排序推荐列表，评分越高排越前面
     */
    private double score;

    /**
     * 推荐理由 —— 人类可读的推荐说明
     *
     * 示例：
     * - "与已选符文形成精密套装，胜率提升8%"
     * - "该英雄胜率最高的符文，胜率52.3%"
     * - "⚠️ 版本陷阱：选取率高达25%但胜率仅47%"
     *
     * 前端在推荐卡片下方显示此文字
     */
    private String recommendationReason;
}