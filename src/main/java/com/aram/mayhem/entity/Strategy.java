package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略/出装方案实体类 —— 对应数据库中的 tb_strategy 表
 *
 * 攻略是用户分享的英雄玩法心得，包含标题、描述、推荐符文和出装等。
 * 其他用户可以给攻略点赞或踩，帮助筛选出高质量的攻略。
 *
 * 这个类是攻略的核心数据，关联的符文和装备分别存储在：
 * - StrategyAugment：攻略推荐的符文（多对多关联）
 * - StrategyItem：攻略推荐的装备（一对多关联）
 *
 * 逻辑删除说明：
 * 使用 @TableLogic 注解实现逻辑删除，删除攻略时不会真正从数据库删除记录，
 * 而是将 deleted 字段设为 1。查询时 MyBatis-Plus 会自动过滤已删除的记录。
 * 这样可以防止误删，也方便数据恢复。
 *
 * 数据流向：
 * 数据库 tb_strategy 表 → MyBatis-Plus 映射为 Strategy 对象
 * → StrategyService 处理业务逻辑
 * → StrategyController 返回给前端（转换为 StrategyListVO/StrategyDetailVO 格式）
 *
 * 关联类：
 * - StrategyController：攻略相关的 API 接口
 * - StrategyService/StrategyServiceImpl：攻略的业务逻辑
 * - StrategyMapper：攻略的数据库操作接口
 * - StrategyAugment：攻略关联的符文
 * - StrategyItem：攻略关联的装备
 * - Vote：攻略的投票记录
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_strategy") // 对应数据库表 tb_strategy
public class Strategy {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布用户 ID —— 外键，关联 tb_user 表的 id
     * 表示这个攻略是谁发布的
     */
    private Long userId;

    /**
     * 关联英雄 ID —— 外键，关联 tb_hero 表的 id
     * 表示这个攻略是针对哪个英雄的
     */
    private Long heroId;

    /** 攻略标题 —— 简短描述攻略的核心内容，如 "盖伦暴力输出流" */
    private String title;

    /** 攻略描述/正文 —— 详细的玩法说明，包括打法思路、注意事项等 */
    private String description;

    /**
     * 点赞数 —— 该攻略收到的点赞总数
     * 由 StrategyService 在投票时自动更新，不直接修改
     */
    private Integer upvotes;

    /**
     * 踩数 —— 该攻略收到的踩总数
     * 由 StrategyService 在投票时自动更新，不直接修改
     */
    private Integer downvotes;

    /**
     * 创建时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT) 表示只在插入时自动填充
     * 创建时间不应该被修改，所以用 INSERT 而不是 INSERT_UPDATE
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 表示在插入和更新时都自动填充
     * 每次修改攻略内容时，这个时间会自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记 —— 1=已删除, 0=正常
     *
     * @TableLogic 注解告诉 MyBatis-Plus 这是逻辑删除字段：
     * - 执行 delete 操作时，实际执行 UPDATE SET deleted=1
     * - 执行 select 操作时，自动加上 WHERE deleted=0 条件
     *
     * 好处：删除的数据可以恢复，也不会影响关联数据
     */
    @TableLogic
    private Integer deleted;
}
