package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略/出装方案实体类
 *
 * 对应表：tb_strategy
 * 数据流向：MyBatis-Plus ↔ StrategyService ↔ StrategyController → StrategyListVO/StrategyDetailVO
 * 关联：StrategyController, StrategyService, StrategyMapper
 */
@Data
@TableName("tb_strategy")
public class Strategy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户 ID */
    private Long userId;

    /** 关联英雄 ID */
    private Long heroId;

    /** 攻略标题 */
    private String title;

    /** 攻略描述/正文 */
    private String description;

    /** 点赞数 */
    private Integer upvotes;

    /** 踩数 */
    private Integer downvotes;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（1=已删除, 0=正常） */
    @TableLogic
    private Integer deleted;
}
