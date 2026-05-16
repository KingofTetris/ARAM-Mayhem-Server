package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 攻略列表视图对象
 *
 * 数据流向：StrategyService → StrategyController → 前端社区攻略列表
 * 用途：社区攻略卡片展示
 */
@Data
public class StrategyListVO {
    private Long id;
    /** 发布用户 ID */
    private Long userId;
    /** 作者昵称 */
    private String authorNickname;
    /** 作者头像 URL */
    private String authorAvatar;
    /** 关联英雄 ID */
    private Long heroId;
    /** 英雄中文名 */
    private String heroName;
    /** 英雄图标 URL */
    private String heroIcon;
    /** 攻略标题 */
    private String title;
    /** 攻略描述 */
    private String description;
    /** 点赞数 */
    private Integer upvotes;
    /** 踩数 */
    private Integer downvotes;
    /** 综合评分（upvotes - downvotes） */
    private Integer score;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 符文图标 URL 列表 */
    private List<String> augmentIcons;
    /** 装备图标 URL 列表 */
    private List<String> itemIcons;
}