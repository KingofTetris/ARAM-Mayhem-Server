package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 强化符文实体类 —— 对应数据库中的 tb_augment 表
 *
 * 强化符文（Augment）是 ARAM 模式中的特殊增益效果。
 * 每局游戏开始时，玩家可以从随机出现的符文中选择一个来增强自己的英雄。
 * 符文有不同的品质（银色/金色/棱彩），品质越高效果越强。
 *
 * 这个类存储了每个符文的基本信息和 ARAM 模式下的统计数据，
 * 用于前端展示符文推荐和胜率排行。
 *
 * 数据流向：
 * 数据库 tb_augment 表 → MyBatis-Plus 映射为 Augment 对象 → Service 层处理
 * → Controller 层返回给前端（转换为 AugmentVO/AugmentListVO 格式）
 *
 * 关联类：
 * - AugmentController：符文相关的 API 接口
 * - AugmentService/AugmentServiceImpl：符文的业务逻辑
 * - AugmentMapper：符文的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName(value = "tb_augment", autoResultMap = true) // 对应数据库表 tb_augment
public class Augment {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 符文中文名 —— 如 "狂战之刃"，用于前端中文显示 */
    private String nameZh;

    /** 符文英文名 —— 如 "Warlord's Blade"，用于数据匹配和搜索 */
    private String nameEn;

    /** 符文描述/效果说明 —— 详细描述符文的具体效果 */
    private String description;

    /**
     * 符文品质 —— 决定符文的稀有度和强度
     * - 银色（Silver）：基础品质，效果较弱
     * - 金色（Gold）：中等品质，效果适中
     * - 棱彩（Prismatic）：最高品质，效果最强
     */
    private String quality;

    /**
     * 第一套装名称 —— 符文可以属于某个"套装"
     * 集齐同一套装的多个符文可以触发套装效果
     * 如 "精密" 套装、"主宰" 套装
     */
    private String synergySet;

    /** 第二套装名称 —— 一个符文可以同时属于多个套装，此字段可为 null */
    private String synergySet2;

    /** 第三套装名称 —— 同上，可为 null */
    private String synergySet3;

    /** 符文图标 URL —— 前端用这个地址加载符文图标 */
    private String iconUrl;

    /**
     * ARAM 胜率 —— 百分比格式
     * 如 52.30 表示选择该符文后有 52.30% 的胜率
     * 使用 BigDecimal 保证精度
     */
    private BigDecimal winRate;

    /**
     * ARAM 选取率 —— 百分比格式
     * 如 15.30 表示 15.30% 的玩家选择了该符文
     */
    private BigDecimal pickRate;

    /**
     * 平均排名 —— 1~8 之间，越小越好
     * 表示选择该符文后，在 8 人对局中的平均排名
     * 如 3.5 表示平均排名第 3.5 名
     */
    private BigDecimal avgPlacement;

    /** 梯级评级 —— 符文强度分级，如 S+/S/A/B/C */
    private String tier;

    /**
     * 是否为陷阱符文 —— 数据表现差的符文
     * 陷阱符文看起来可能不错，但实际胜率很低，不推荐选择
     */
    private Boolean isTrap;

    /**
     * 是否为版本陷阱符文 —— 被游戏版本大幅削弱的符文
     * 与 isTrap 的区别：isTrap 是一直表现差，isVersionTrap 是某个版本被削弱后才变差
     */
    private Boolean isVersionTrap;

    /** 版本陷阱标记时间 —— 记录何时被标记为版本陷阱，取消标记时置为 null */
    private LocalDateTime versionTrapSince;

    /** 数据版本号 —— 如 14.8，表示数据来自哪个游戏版本 */
    private String version;

    /**
     * 数据更新时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 表示在插入和更新时自动填充
     * 实际填充逻辑在 MyBatisMetaObjectHandler 中实现
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
