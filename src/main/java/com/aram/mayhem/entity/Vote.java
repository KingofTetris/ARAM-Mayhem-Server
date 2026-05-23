package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票记录实体类 —— 对应数据库中的 tb_vote 表
 *
 * 这个类记录了用户对攻略的投票（点赞或踩）。
 * 每个用户对每个攻略只能投一次票，再次投票会更新之前的记录。
 *
 * 投票类型：
 * - up（点赞）：表示用户认为这个攻略有用
 * - down（踩）：表示用户认为这个攻略不好
 *
 * 防重复投票机制：
 * 通过 userId + strategyId 的唯一约束，确保一个用户对同一个攻略只能有一条投票记录。
 * 如果用户已经点过赞，再次点击会切换为踩（反之亦然）。
 *
 * 数据流向：
 * 数据库 tb_vote 表 → MyBatis-Plus 映射为 Vote 对象
 * → StrategyService 处理投票逻辑（点赞/踩/取消）
 * → VoteController 提供 API 接口
 *
 * 关联类：
 * - VoteController：投票相关的 API 接口
 * - StrategyService：投票的业务逻辑
 * - VoteMapper：投票记录的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_vote") // 对应数据库表 tb_vote
public class Vote {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投票用户 ID —— 外键，关联 tb_user 表的 id
     * 表示是哪个用户投的票
     */
    private Long userId;

    /**
     * 被投票的攻略 ID —— 外键，关联 tb_strategy 表的 id
     * 表示对哪个攻略投的票
     */
    private Long strategyId;

    /**
     * 投票类型 —— 只有两个值
     * - "up"：点赞，表示认可这个攻略
     * - "down"：踩，表示不认可这个攻略
     */
    private String voteType;

    /**
     * 投票时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT) 表示只在插入时自动填充
     * 投票时间不需要在更新时修改，所以用 INSERT 而不是 INSERT_UPDATE
     * 实际填充逻辑在 MyBatisMetaObjectHandler 中实现
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
