package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 攻略详情视图对象
 *
 * 数据流向：StrategyService → StrategyController → 前端攻略详情页
 * 用途：攻略完整信息展示，含符文和装备列表
 */
@Data
public class StrategyDetailVO {
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
    /** 攻略正文 */
    private String description;
    /** 点赞数 */
    private Integer upvotes;
    /** 踩数 */
    private Integer downvotes;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 推荐符文列表 */
    private List<AugmentVO> augments;
    /** 推荐装备列表 */
    private List<ItemVO> items;
    /** 当前用户的投票类型（UP/DOWN/null=未投票） */
    private String userVoteType;
}