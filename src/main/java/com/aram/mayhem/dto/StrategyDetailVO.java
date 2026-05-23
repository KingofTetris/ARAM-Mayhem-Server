package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 攻略详情视图对象 —— 攻略详情页展示用
 *
 * 与 StrategyListVO 的区别：
 * - StrategyListVO：列表页卡片，只有摘要信息
 * - StrategyDetailVO：详情页，包含完整的攻略内容、符文列表、装备列表
 *
 * 额外字段说明：
 * - augments：推荐的符文完整信息（列表页只显示图标）
 * - items：推荐的装备完整信息（列表页只显示图标）
 * - userVoteType：当前用户对这个攻略的投票状态，前端用这个显示已点赞/已踩的状态
 *
 * 数据流向：
 * StrategyService 查询攻略详情 + 关联数据 → 组装为 StrategyDetailVO → 返回给前端
 *
 * 关联类：
 * - StrategyListVO：列表页视图对象
 * - AugmentVO：符文详情视图对象
 * - ItemVO：装备视图对象
 */
@Data
public class StrategyDetailVO {

    /** 攻略 ID */
    private Long id;

    /** 发布用户 ID —— 前端判断是否显示"编辑/删除"按钮 */
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

    /** 攻略正文 —— 详细的玩法说明 */
    private String description;

    /** 点赞数 */
    private Integer upvotes;

    /** 踩数 */
    private Integer downvotes;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 推荐符文列表 —— 包含每个符文的完整信息 */
    private List<AugmentVO> augments;

    /** 推荐装备列表 —— 包含每个装备的完整信息 */
    private List<ItemVO> items;

    /**
     * 当前用户的投票类型 —— 用于前端显示投票状态
     * - "UP"：当前用户已点赞，按钮显示为已激活状态
     * - "DOWN"：当前用户已踩，按钮显示为已激活状态
     * - null：当前用户未投票，按钮显示为未激活状态
     * 未登录用户此字段始终为 null
     */
    private String userVoteType;
}
