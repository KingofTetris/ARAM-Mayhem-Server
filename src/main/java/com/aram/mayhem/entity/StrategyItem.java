package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 攻略-出装物品关联实体类 —— 对应数据库中的 tb_strategy_item 表
 *
 * 这个类记录了"某个攻略推荐了哪些装备"。
 * 与 StrategyAugment 类似，这也是一个关联表，但关联的是装备而不是符文。
 *
 * 每条记录不仅包含攻略 ID 和物品名称，还包含物品分类和排序信息，
 * 这样前端可以按照"起始装备 → 核心装备 → 可选装备"的顺序展示。
 *
 * 举个例子：
 * - 攻略 ID=1（盖伦出装攻略）推荐了：
 *   - 多兰盾（起始装备，排序 1）
 *   - 黑色切割者（核心装备，排序 2）
 *   - 斯特拉克的挑战护手（核心装备，排序 3）
 *
 * 数据流向：
 * 数据库 tb_strategy_item 表 → MyBatis-Plus 映射为 StrategyItem 对象
 * → StrategyService 查询攻略详情时，通过 strategyId 查询关联的装备
 * → 组装到 StrategyDetailVO.items 中返回给前端
 *
 * 关联类：
 * - StrategyService：查询攻略关联的装备
 * - StrategyItemMapper：关联表的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_strategy_item") // 对应数据库表 tb_strategy_item
public class StrategyItem {

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
     * 物品名称 —— 装备的中文名称
     * 如 "黑色切割者"、"斯特拉克的挑战护手"
     * 注意：这里直接存名称而不是 ID，因为装备数据不在本系统中管理
     */
    private String itemName;

    /**
     * 物品分类 —— 装备在出装方案中的角色
     * - 起始装备：游戏开始时购买的便宜装备
     * - 核心装备：必须优先出的关键装备
     * - 可选装备：根据局势灵活选择的装备
     */
    private String itemCategory;

    /**
     * 排序序号 —— 同一分类下的显示顺序
     * 数值越小越靠前，如 sortOrder=1 排在 sortOrder=2 前面
     */
    private Integer sortOrder;
}
