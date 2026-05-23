package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 英雄实体类 —— 对应数据库中的 tb_hero 表
 *
 * 这个类是"英雄"这个业务概念在代码中的表示。
 * 数据库里的 tb_hero 表存储了所有英雄的数据，这个类就是那张表的"Java 翻译版"。
 * 每一个字段对应数据库表中的一列，MyBatis-Plus 会自动完成对象和表记录的互相转换。
 *
 * 数据流向：
 * 数据库 tb_hero 表 → MyBatis-Plus 自动映射为 Hero 对象 → Service 层处理业务逻辑
 * → Controller 层返回给前端（转换为 VO 格式）
 *
 * 关联类：
 * - HeroController：英雄相关的 API 接口
 * - HeroService/HeroServiceImpl：英雄的业务逻辑
 * - HeroMapper：英雄的数据库操作接口
 *
 * 注解说明：
 * - @Data：Lombok 注解，自动生成 getter/setter/toString/equals/hashCode 方法
 * - @TableName：指定对应的数据库表名
 * - autoResultMap = true：启用自动结果映射，支持 TypeHandler（如 JSON 字段的序列化）
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName(value = "tb_hero", autoResultMap = true) // 对应数据库表 tb_hero，启用自动结果映射
public class Hero {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增（AUTO_INCREMENT）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Riot 官方英雄 ID —— 来自 Riot Games 的唯一标识符，如 266 代表亚托克斯 */
    private Integer riotId;

    /** 英雄英文名 —— 如 Aatrox（亚托克斯）、Ahri（阿狸） */
    private String nameEn;

    /** 英雄中文名 —— 如 亚托克斯、阿狸，用于前端显示 */
    private String nameZh;

    /** 英雄称号 —— 如 暗裔剑魔、九尾妖狐，显示在英雄详情页 */
    private String title;

    /** 英雄定位 —— 如 战士、法师、坦克、刺客、射手、辅助 */
    private String role;

    /** 英雄头像/图标 URL —— 前端用这个地址加载英雄头像图片 */
    private String imageUrl;

    /** 梯级评级 —— 英雄强度分级，如 S+（最强）、S、A、B、C（最弱） */
    private String tier;

    /**
     * ARAM 胜率 —— 百分比格式
     * 如 52.30 表示 52.30% 的胜率
     * 使用 BigDecimal 而不是 double，避免浮点数精度丢失问题
     */
    private BigDecimal winRate;

    /**
     * ARAM 选取率 —— 百分比格式
     * 如 15.30 表示 15.30% 的选取率
     */
    private BigDecimal pickRate;

    /** 数据置信度等级 —— 如 high（高置信度）、medium（中等）、low（低） */
    private String confidenceLevel;

    /** 英雄描述/简介 —— 一段简短的文字介绍英雄特点 */
    private String description;

    /**
     * 技能列表 —— JSON 格式存储
     *
     * 数据库中存储为 JSON 字符串，如：
     * [{"key":"Q","name":"暗裔利刃","description":"亚托克斯挥舞巨剑..."}, ...]
     *
     * JacksonTypeHandler 负责在读取时将 JSON 字符串转为 List<SkillData>，
     * 在保存时将 List<SkillData> 转回 JSON 字符串。
     */
    @TableField(typeHandler = JacksonTypeHandler.class) // 指定 JSON 类型处理器
    private List<SkillData> skills;

    /** 克制技巧列表 —— JSON 格式存储，如 ["避免近身对拼","利用远程消耗"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> counterTips;

    /** 协同英雄列表 —— JSON 格式存储，如 ["阿狸","娑娜"]，表示与这些英雄配合效果好 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> synergies;

    /** 场均击杀数 —— 如 5.30 表示平均每局击杀 5.3 次 */
    private BigDecimal avgKills;

    /** 场均死亡数 —— 如 6.10 表示平均每局死亡 6.1 次 */
    private BigDecimal avgDeaths;

    /** 场均助攻数 —— 如 8.40 表示平均每局助攻 8.4 次 */
    private BigDecimal avgAssists;

    /** 推荐出装 —— JSON 格式存储，包含推荐的装备组合 */
    private String recommendedBuild;

    /** 推荐强化符文 ID 列表 —— JSON 格式存储，关联 tb_augment 表的 ID */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> recommendedAugmentIds;

    /** 数据版本号 —— 如 14.8，表示数据来自哪个游戏版本 */
    private String version;

    /** 是否为版本陷阱英雄 —— 被大幅削弱的英雄，表面数据好但实际不推荐 */
    private Boolean isVersionTrap;

    /** 版本陷阱标记时间 —— 记录何时被标记为版本陷阱，取消标记时置为 null */
    private LocalDateTime versionTrapSince;

    /**
     * 数据更新时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 表示在插入和更新时自动填充
     * 实际填充逻辑在 MyBatisMetaObjectHandler 中实现
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 技能数据结构 —— 英雄技能的内部类
     *
     * 每个英雄有 5 个技能：
     * - 被动技能（Passive）
     * - Q 技能
     * - W 技能
     * - E 技能
     * - R 技能（大招）
     *
     * 这个内部类定义了每个技能的数据结构，
     * 存储在 Hero 的 skills 字段中（JSON 格式）。
     */
    @Data
    public static class SkillData {
        /** 技能按键 —— Q/W/E/R/被动 */
        private String key;
        /** 技能名称 —— 如 暗裔利刃 */
        private String name;
        /** 技能描述 —— 技能效果的详细说明 */
        private String description;
    }
}
