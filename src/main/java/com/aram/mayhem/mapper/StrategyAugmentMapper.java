package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyAugment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略-符文关联数据访问接口（Mapper） —— 攻略符文关联数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"攻略-符文关联"数据和数据库之间的桥梁。
 * 它管理的是攻略中推荐的符文列表，记录了"哪个攻略推荐了哪些符文"。
 *
 * 为什么需要一张单独的关联表？
 * 因为攻略和符文之间是"多对多"关系：
 * - 一个攻略可以推荐多个符文（比如推荐 3 个符文组合）
 * - 一个符文可以被多个攻略推荐（比如"狂战之刃"被很多攻略推荐）
 *
 * 多对多关系不能直接在任一方的表中存储，必须用一张中间表来建立关联。
 * tb_strategy_augment 就是这张中间表，每条记录代表"某攻略推荐了某符文"。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<StrategyAugment> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法 —— 最常用的操作】
 * - selectById(id)              → 根据 ID 查询单条关联记录
 * - selectList(wrapper)         → 根据条件查询关联记录列表
 *   ★ 最常用：按 strategyId 查询某个攻略推荐的所有符文
 *
 * 【新增方法】
 * - insert(strategyAugment)     → 插入一条攻略-符文关联记录
 *
 * 【修改方法】
 * - updateById(strategyAugment) → 根据 ID 更新关联记录
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除单条关联记录
 * - delete(wrapper)             → 根据条件删除（如删除某个攻略的所有符文关联）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与其他 Mapper 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyAugment 是 Strategy 和 Augment 之间的中间表：
 *
 *   tb_strategy（攻略表）   tb_strategy_augment（关联表）   tb_augment（符文表）
 *   ┌────┬──────────┐      ┌────┬────────────┬──────────┐   ┌────┬──────────┐
 *   │ id │ title    │      │ id │strategyId  │augmentId │   │ id │nameZh    │
 *   ├────┼──────────┤      ├────┼────────────┼──────────┤   ├────┼──────────┤
 *   │  1 │盖伦出装  │◄─────│  1 │  1         │  5       │──►│  5 │狂战之刃  │
 *   │    │          │◄─────│  2 │  1         │  12      │──►│ 12 │征服者    │
 *   └────┴──────────┘      └────┴────────────┴──────────┘   └────┴──────────┘
 *
 * 查询流程：
 * 1. StrategyService 查询攻略详情时，先通过 StrategyMapper 查询攻略基本信息
 * 2. 通过 StrategyAugmentMapper 按 strategyId 查询该攻略关联的符文 ID 列表
 * 3. 通过 AugmentMapper 按 ID 列表查询符文的详细信息
 * 4. 将符文列表组装到 StrategyDetailVO.augments 中返回给前端
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Mapper：告诉 MyBatis "这是一个 Mapper 接口，请为它创建实现类"。
 *          Spring Boot 启动时会扫描所有 @Mapper 注解的接口，
 *          自动为每个接口生成一个代理类，代理类包含了所有 SQL 操作。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据库 tb_strategy_augment 表
 *     ↕（MyBatis-Plus 自动映射）
 * StrategyAugmentMapper（本接口）
 *     ↕（StrategyService 调用 Mapper 方法）
 * StrategyService / StrategyServiceImpl
 *     ↕（查询关联的 Augment 详情，组装到 StrategyDetailVO.augments 中）
 * StrategyController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（攻略详情页展示推荐符文）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - StrategyAugment：攻略-符文关联实体类，对应 tb_strategy_augment 表
 * - Strategy：攻略实体类，关联通过 strategyId 字段
 * - Augment：符文实体类，关联通过 augmentId 字段
 * - StrategyService / StrategyServiceImpl：攻略业务逻辑层
 * - BaseMapper<StrategyAugment>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询某个攻略推荐的所有符文 ID（最常用）
 *   LambdaQueryWrapper<StrategyAugment> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(StrategyAugment::getStrategyId, strategyId);
 *   List<StrategyAugment> augmentLinks = strategyAugmentMapper.selectList(wrapper);
 *
 *   // 2. 从关联记录中提取符文 ID 列表，再查询符文详情
 *   List<Long> augmentIds = augmentLinks.stream()
 *       .map(StrategyAugment::getAugmentId)
 *       .collect(Collectors.toList());
 *   List<Augment> augments = augmentMapper.selectBatchIds(augmentIds);
 *
 *   // 3. 发布攻略时批量插入符文关联
 *   for (Long augmentId : augmentIds) {
 *       StrategyAugment link = new StrategyAugment();
 *       link.setStrategyId(strategyId);
 *       link.setAugmentId(augmentId);
 *       strategyAugmentMapper.insert(link);
 *   }
 *
 *   // 4. 更新攻略时先删除旧关联，再插入新关联
 *   wrapper.eq(StrategyAugment::getStrategyId, strategyId);
 *   strategyAugmentMapper.delete(wrapper);  // 删除旧关联
 *   // 然后插入新关联...
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface StrategyAugmentMapper extends BaseMapper<StrategyAugment> {
    // 当前不需要自定义方法，BaseMapper<StrategyAugment> 已提供所有基础 CRUD 操作
    // 最常用的操作是按 strategyId 查询符文关联列表，通过 LambdaQueryWrapper 即可实现
}
