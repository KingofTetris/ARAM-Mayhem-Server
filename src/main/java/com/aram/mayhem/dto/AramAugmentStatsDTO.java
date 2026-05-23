package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARAM 强化符文统计数据 DTO —— 从 U.GG 采集的符文 ARAM 统计数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类封装了从 U.GG 网站采集到的 ARAM 模式符文统计数据。
 * 每个对象代表一个符文在 ARAM 模式下的表现数据，包括胜率、选取率、排名等。
 *
 * 就像一张"符文成绩单"：
 * - augmentName = 学生姓名
 * - quality     = 年级（银色/金色/棱彩）
 * - winRate     = 考试及格率
 * - pickRate    = 选课人数比例
 * - avgPlacement= 班级排名
 * - tier        = 综合评级（S+/S/A/B/C）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据来源
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据来源：U.GG 网站 ARAM Mayhem 页面
 * 采集方式：Jsoup HTML 解析（AramDataCollector 类）
 * 采集频率：每天凌晨 3:00 自动采集（DataSyncScheduler 定时任务）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * U.GG 网站（HTML 页面）
 *     ↓ Jsoup 解析
 * AramDataCollector.collectAugmentStats() → 生成 AramAugmentStatsDTO
 *     ↓ 传递给聚合器
 * DataAggregatorServiceImpl.aggregateAugmentData() → 聚合多源数据
 *     ↓ 写入数据库
 * AugmentMapper.insert/update → MySQL t_augment 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数值格式说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有百分比字段均为百分比格式，使用 BigDecimal 保证精度：
 * - winRate = 52.30 表示 52.30%（不是 0.5230）
 * - pickRate = 15.30 表示 15.30%（不是 0.1530）
 *
 * 为什么用 BigDecimal 而不用 double？
 * - double 存在浮点精度问题：0.1 + 0.2 = 0.30000000000000004
 * - BigDecimal 可以精确表示小数，适合金融和统计场景
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AramAugmentStatsDTO {

    /**
     * 符文名称 —— 符文的英文标识名
     *
     * 示例："Prowler's Claw"、"Dark Harvest"
     * 用于与数据库中的 augment 记录进行名称匹配
     */
    private String augmentName;

    /**
     * 符文品质 —— 符文的稀有度等级
     *
     * ARAM Mayhem 模式中符文分三个品质：
     * - "银色"（Silver）：基础符文，效果较弱
     * - "金色"（Gold）：进阶符文，效果中等
     * - "棱彩"（Prismatic）：顶级符文，效果最强
     *
     * 品质影响：
     * - 品质越高，出现概率越低
     * - 品质越高，属性加成越强
     * - 前端根据品质显示不同颜色的边框
     */
    private String quality;

    /**
     * ARAM 胜率 —— 使用该符文时的胜率
     *
     * 百分比格式，如 52.30 表示 52.30%
     * 胜率 = 获胜场次 / 总场次 × 100
     *
     * 评判标准：
     * - > 54%：强势符文（S 级）
     * - 50%~54%：均衡符文（A/B 级）
     * - < 50%：弱势符文（C 级，可能是版本陷阱）
     */
    private BigDecimal winRate;

    /**
     * ARAM 选取率 —— 该符文被选择的概率
     *
     * 百分比格式，如 15.30 表示 15.30%
     * 选取率 = 选择该符文的次数 / 总选择次数 × 100
     *
     * 评判标准：
     * - > 20%：热门符文
     * - 10%~20%：常规符文
     * - < 10%：冷门符文
     */
    private BigDecimal pickRate;

    /**
     * 平均排名 —— 使用该符文时在队伍中的平均名次
     *
     * 范围：1~8 之间，越小越好
     * - 1.0~2.5：经常拿第一第二，非常强
     * - 2.5~4.0：中上水平
     * - 4.0~6.0：中等水平
     * - 6.0~8.0：经常垫底，较弱
     */
    private BigDecimal avgPlacement;

    /**
     * ARAM 梯级评级 —— 综合胜率和选取率的等级评定
     *
     * 取值范围：S+ / S / A / B / C
     * - S+：顶级符文，胜率和选取率都很高
     * - S：强势符文
     * - A：优秀符文
     * - B：普通符文
     * - C：弱势符文，可能是版本陷阱
     *
     * 评级由 U.GG 算法计算，综合考虑胜率、选取率、排名等因素
     */
    private String tier;

    /**
     * 套装名称 —— 该符文所属的套装（协同效果组）
     *
     * 示例："精密"、"主宰"、"巫术"
     *
     * 套装机制：
     * - 同一套装的符文凑齐一定数量后，会激活额外的套装效果
     * - 例如"精密"套装需要3个精密符文才能激活
     * - 前端 SynergyProgressResponse 展示套装激活进度
     */
    private String synergySet;

    /**
     * 数据来源标识 —— 标记数据来自哪个网站
     *
     * 固定值："u.gg"
     * 用于 MultiSourceValidator 交叉验证时区分不同数据源
     * 未来可能新增更多数据源（如 lolalytics.com）
     */
    private String source;
}
