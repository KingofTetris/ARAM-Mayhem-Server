package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票记录实体类
 *
 * 对应表：tb_vote
 * 数据流向：MyBatis-Plus ↔ StrategyService ↔ VoteController
 * 关联：VoteController, StrategyService, VoteMapper
 */
@Data
@TableName("tb_vote")
public class Vote {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投票用户 ID */
    private Long userId;

    /** 被投票的攻略 ID */
    private Long strategyId;

    /** 投票类型（up=点赞, down=踩） */
    private String voteType;

    /** 投票时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
