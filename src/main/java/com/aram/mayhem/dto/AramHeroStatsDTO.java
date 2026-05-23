package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARAM 英雄统计数据 DTO —— 从 U.GG 采集的英雄 ARAM 统计数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类封装了从 U.GG 网站采集到的 ARAM 模式英雄统计数据。
 * 每个对象代表一个英雄在 ARAM 模式下的表现数据，包括胜率、选取率、KDA 等。
 *
 * 与 AramAugmentStatsDTO 的区别：
 * - AramHeroStatsDTO：记录英雄的统计数据（胜率、KDA）
 * - AramAugmentStatsDTO：记录符文的统计数据（胜率、套装）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据来源
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据来源：U.GG 网站 ARAM 页面
 * 采集方式：Jsoup HTML 解析（AramDataCollector 类）
 * 采集频率：每天凌晨 3:00 自动采集（DataSyncScheduler 定时任务）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * U.GG 网站（HTML 页面）
 *     ↓ Jsoup 解析
 * AramDataCollector.collectHeroStats() → 生成 AramHeroStatsDTO
 *     ↓ 传递给聚合器
 * DataAggregatorServiceImpl.aggregateHeroData() → 聚合多源数据
 *     ↓ 与 Riot DataDragon 数据关联
 * 通过 championName 匹配 Riot 的英雄基础数据（名称、头像、技能等）
 *     ↓ 写入数据库
 * HeroMapper.insert/update → MySQL t_hero 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、多源数据关联
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本项目的英雄数据来自两个数据源：
 * 1. Riot DataDragon：英雄基础信息（名称、头像、技能描述）
 * 2. U.GG：英雄 ARAM 统计数据（胜率、KDA）
 *
 * 关联方式：通过 championName（英雄英文名）进行匹配
 * - Riot 数据的 key 字段 = "Aatrox"
 * - U.GG 数据的 championName = "Aatrox"
 * - 两者匹配后，合并为完整的英雄数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数值格式说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有百分比字段均为百分比格式，使用 BigDecimal 保证精度：
 * - winRate = 52.30 表示 52.30%（不是 0.5230）
 * - pickRate = 15.30 表示 15.30%（不是 0.1530）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AramHeroStatsDTO {

    /**
     * 英雄英文名 —— 英雄的英文标识名，用于多源数据关联
     *
     * 示例："Aatrox"、"Ahri"、"Akali"
     *
     * 这个字段是关联 Riot DataDragon 数据的关键：
     * - Riot DataDragon 使用 key 字段标识英雄（如 "Aatrox"）
     * - U.GG 使用相同的英文名标识英雄
     * - 两个数据源通过 championName 进行匹配和合并
     *
     * 注意：英文名大小写敏感，必须与 Riot DataDragon 完全一致
     */
    private String championName;

    /**
     * ARAM 梯级评级 —— 综合胜率和选取率的等级评定
     *
     * 取值范围：S+ / S / A / B / C
     * - S+：顶级英雄，胜率极高（> 54%）
     * - S：强势英雄，胜率较高（52%~54%）
     * - A：优秀英雄，胜率均衡（50%~52%）
     * - B：普通英雄，胜率略低（48%~50%）
     * - C：弱势英雄，胜率较低（< 48%），可能是版本陷阱
     *
     * 评级由 U.GG 算法计算，综合考虑胜率、选取率、场次等因素
     */
    private String tier;

    /**
     * ARAM 胜率 —— 该英雄在 ARAM 模式中的胜率
     *
     * 百分比格式，如 52.30 表示 52.30%
     * 胜率 = 获胜场次 / 总场次 × 100
     *
     * 评判标准：
     * - > 54%：版本强势英雄（S 级）
     * - 50%~54%：均衡英雄（A/B 级）
     * - < 50%：弱势英雄（C 级，可能是版本陷阱）
     *
     * 版本陷阱（isVersionTrap）：
     * 当一个英雄选取率很高但胜率很低时，会被标记为版本陷阱，
     * 提醒玩家"这个英雄看起来很强，实际上胜率很低"
     */
    private BigDecimal winRate;

    /**
     * ARAM 选取率 —— 该英雄在 ARAM 模式中被选择的概率
     *
     * 百分比格式，如 15.30 表示 15.30%
     * 选取率 = 选择该英雄的次数 / 总选择次数 × 100
     *
     * 评判标准：
     * - > 20%：热门英雄（如亚索、提莫）
     * - 10%~20%：常规英雄
     * - < 10%：冷门英雄
     */
    private BigDecimal pickRate;

    /**
     * 场均击杀数 —— 该英雄每场平均击杀敌方英雄的数量
     *
     * 示例：5.2 表示平均每场击杀 5.2 个敌方英雄
     *
     * 用途：
     * - 前端英雄详情页展示 KDA 数据
     * - 用于计算 KDA 评分：(kills + assists) / deaths
     * - 帮助玩家了解英雄的击杀能力
     */
    private BigDecimal avgKills;

    /**
     * 场均死亡数 —— 该英雄每场平均被击杀的次数
     *
     * 示例：4.8 表示平均每场死亡 4.8 次
     *
     * 用途：
     * - 前端英雄详情页展示 KDA 数据
     * - 死亡数越低，说明英雄生存能力越强
     * - 死亡数为 0 时 KDA 为完美评分（显示 "Perfect"）
     */
    private BigDecimal avgDeaths;

    /**
     * 场均助攻数 —— 该英雄每场平均助攻队友的次数
     *
     * 示例：8.5 表示平均每场助攻 8.5 次
     *
     * 用途：
     * - 前端英雄详情页展示 KDA 数据
     * - 辅助型英雄通常助攻数较高
     * - 用于计算 KDA 评分：(kills + assists) / deaths
     */
    private BigDecimal avgAssists;

    /**
     * 数据来源标识 —— 标记数据来自哪个网站
     *
     * 固定值："u.gg"
     * 用于 MultiSourceValidator 交叉验证时区分不同数据源
     * 未来可能新增更多数据源（如 lolalytics.com、op.gg）
     */
    private String source;
}
