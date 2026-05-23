package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 攻略列表视图对象 —— 社区攻略列表页展示用
 *
 * 这个 VO 包含了攻略卡片需要的所有信息：
 * - 攻略基本信息（标题、描述、评分）
 * - 作者信息（昵称、头像）
 * - 英雄信息（名称、图标）
 * - 关联信息（符文图标、装备图标）
 *
 * 与 Strategy 实体的区别：
 * - Strategy 只存储核心数据（userId、heroId 等外键 ID）
 * - StrategyListVO 通过 JOIN 查询把 ID 替换为实际的名称和图标
 *
 * 数据流向：
 * StrategyService 查询攻略列表 → 组装为 StrategyListVO → 返回给前端
 *
 * 关联类：
 * - Strategy：数据库实体类
 * - StrategyDetailVO：攻略详情视图对象
 */
@Data
public class StrategyListVO {

    /** 攻略 ID —— 唯一标识，点击时跳转详情页 */
    private Long id;

    /** 发布用户 ID —— 前端用于判断是否是当前用户的攻略 */
    private Long userId;

    /** 作者昵称 —— 从 User 表 JOIN 查询得到 */
    private String authorNickname;

    /** 作者头像 URL —— 从 User 表 JOIN 查询得到 */
    private String authorAvatar;

    /** 关联英雄 ID —— 前端用于跳转到英雄详情页 */
    private Long heroId;

    /** 英雄中文名 —— 从 Hero 表 JOIN 查询得到 */
    private String heroName;

    /** 英雄图标 URL —— 从 Hero 表 JOIN 查询得到 */
    private String heroIcon;

    /** 攻略标题 —— 卡片上显示的标题 */
    private String title;

    /** 攻略描述 —— 卡片上显示的摘要 */
    private String description;

    /** 点赞数 —— 该攻略收到的点赞总数 */
    private Integer upvotes;

    /** 踩数 —— 该攻略收到的踩总数 */
    private Integer downvotes;

    /** 综合评分 —— upvotes - downvotes，用于排序 */
    private Integer score;

    /** 创建时间 —— 显示"发布于 X 天前" */
    private LocalDateTime createdAt;

    /** 符文图标 URL 列表 —— 卡片底部显示的符文小图标 */
    private List<String> augmentIcons;

    /** 装备图标 URL 列表 —— 卡片底部显示的装备小图标 */
    private List<String> itemIcons;
}
