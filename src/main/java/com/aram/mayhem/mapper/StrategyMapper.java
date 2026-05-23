package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Strategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略数据访问接口（Mapper） —— 攻略数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"攻略/出装方案"数据和数据库之间的桥梁。
 * 当 Service 层需要查询、新增、修改或删除攻略数据时，调用这个接口的方法。
 *
 * 攻略是用户分享的英雄玩法心得，包含：
 * - 标题和描述（攻略内容）
 * - 关联的英雄（针对哪个英雄）
 * - 推荐的符文（通过 StrategyAugment 关联）
 * - 推荐的装备（通过 StrategyItem 关联）
 * - 点赞/踩数（通过 Vote 表统计）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<Strategy> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法】
 * - selectById(id)              → 根据 ID 查询单个攻略
 * - selectBatchIds(idList)      → 根据 ID 列表批量查询多个攻略
 * - selectOne(wrapper)          → 根据条件查询单个攻略
 * - selectList(wrapper)         → 根据条件查询攻略列表
 * - selectPage(page, wrapper)   → 分页查询攻略列表（用于社区攻略列表页）
 * - selectCount(wrapper)        → 统计符合条件的攻略数量
 *
 * 【新增方法】
 * - insert(strategy)            → 插入一条攻略记录
 *
 * 【修改方法】
 * - updateById(strategy)        → 根据 ID 更新攻略信息
 * - update(strategy, wrapper)   → 根据条件更新攻略信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除攻略（逻辑删除，设置 deleted=1）
 * - delete(wrapper)             → 根据条件删除
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、攻略的关联数据
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略是一个"聚合根"，它关联了多张子表的数据：
 *
 *   tb_strategy（攻略主表）
 *       │
 *       ├── tb_strategy_augment（攻略-符文关联表）
 *       │   通过 strategyId 关联，记录攻略推荐的符文
 *       │
 *       ├── tb_strategy_item（攻略-装备关联表）
 *       │   通过 strategyId 关联，记录攻略推荐的装备
 *       │
 *       └── tb_vote（投票记录表）
 *           通过 strategyId 关联，记录用户对攻略的点赞/踩
 *
 * 查询攻略详情时，需要同时查询这些关联数据，组装成完整的 StrategyDetailVO。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、逻辑删除说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * Strategy 实体类使用了 @TableLogic 注解，实现逻辑删除：
 * - 调用 deleteById(id) 时，不会执行 DELETE SQL，而是执行 UPDATE SET deleted=1
 * - 调用 selectList() 等查询方法时，会自动加上 WHERE deleted=0 条件
 * - 这样删除的攻略不会真正消失，方便数据恢复和审计
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Mapper：告诉 MyBatis "这是一个 Mapper 接口，请为它创建实现类"。
 *          Spring Boot 启动时会扫描所有 @Mapper 注解的接口，
 *          自动为每个接口生成一个代理类，代理类包含了所有 SQL 操作。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据库 tb_strategy 表
 *     ↕（MyBatis-Plus 自动映射，逻辑删除自动过滤）
 * StrategyMapper（本接口）
 *     ↕（StrategyService 调用 Mapper 方法）
 * StrategyService / StrategyServiceImpl
 *     ↕（同时查询关联的 StrategyAugment、StrategyItem、Vote）
 * StrategyController / VoteController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（社区攻略列表页、攻略详情页）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Strategy：攻略实体类，对应 tb_strategy 表
 * - StrategyAugment / StrategyAugmentMapper：攻略-符文关联
 * - StrategyItem / StrategyItemMapper：攻略-装备关联
 * - Vote / VoteMapper：攻略投票记录
 * - StrategyService / StrategyServiceImpl：攻略的业务逻辑层
 * - StrategyController：攻略的 API 接口层
 * - BaseMapper<Strategy>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 八、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询某个英雄的所有攻略（按点赞数排序）
 *   LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(Strategy::getHeroId, heroId)
 *          .orderByDesc(Strategy::getUpvotes);
 *   List<Strategy> strategies = strategyMapper.selectList(wrapper);
 *
 *   // 2. 分页查询攻略列表
 *   Page<Strategy> page = new Page<>(1, 10);
 *   Page<Strategy> result = strategyMapper.selectPage(page, wrapper);
 *
 *   // 3. 发布新攻略
 *   Strategy strategy = new Strategy();
 *   strategy.setUserId(userId);
 *   strategy.setHeroId(heroId);
 *   strategy.setTitle("盖伦暴力输出流");
 *   strategy.setDescription("核心装备黑色切割者...");
 *   strategyMapper.insert(strategy);
 *
 *   // 4. 更新攻略点赞数（投票后由 Service 调用）
 *   strategy.setUpvotes(strategy.getUpvotes() + 1);
 *   strategyMapper.updateById(strategy);
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface StrategyMapper extends BaseMapper<Strategy> {
    // 当前不需要自定义方法，BaseMapper<Strategy> 已提供所有基础 CRUD 操作
    // 攻略的复杂查询（如按英雄+排序+分页）通过 LambdaQueryWrapper 即可实现
}
