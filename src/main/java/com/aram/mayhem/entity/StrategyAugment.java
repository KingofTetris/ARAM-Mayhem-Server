package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 攻略-符文关联实体类 —— 对应数据库中的 tb_strategy_augment 表
 *
 * 这是一个"关联表"，用于记录"哪个攻略推荐了哪些符文"。
 * 因为一个攻略可以推荐多个符文，一个符文也可以被多个攻略推荐，
 * 所以需要一张中间表来建立它们之间的多对多关系。
 *
 * 举个例子：
 * - 攻略 ID=1（盖伦出装攻略）推荐了符文 ID=5（狂战之刃）
 * - 那么这张表中就有一条记录：strategyId=1, augmentId=5
 *
 * 数据流向：
 * 数据库 tb_strategy_augment 表 → MyBatis-Plus 映射为 StrategyAugment 对象
 * → StrategyService 查询攻略详情时，通过 strategyId 查询关联的符文
 * → 组装到 StrategyDetailVO.augments 中返回给前端
 *
 * 关联类：
 * - StrategyService：查询攻略关联的符文
 * - StrategyAugmentMapper：关联表的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_strategy_augment") // 对应数据库表 tb_strategy_augment
public class StrategyAugment {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联攻略 ID —— 外键，关联 tb_strategy 表的 id
     * 表示这条记录属于哪个攻略
     */
    private Long strategyId;

    /**
     * 关联符文 ID —— 外键，关联 tb_augment 表的 id
     * 表示这个攻略推荐了哪个符文
     */
    private Long augmentId;
}
