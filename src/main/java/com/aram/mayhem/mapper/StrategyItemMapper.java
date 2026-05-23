package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略-装备关联数据访问接口（Mapper） —— 攻略装备关联数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"攻略-装备关联"数据和数据库之间的桥梁。
 * 它管理的是攻略中推荐的装备列表，记录了"哪个攻略推荐了哪些装备"。
 *
 * 为什么需要一张单独的表？
 * 因为一个攻略可以推荐多个装备，这些装备还有分类和排序信息：
 * - 起始装备：游戏开始时购买的便宜装备（如多兰盾）
 * - 核心装备：必须优先出的关键装备（如黑色切割者）
 * - 可选装备：根据局势灵活选择的装备（如守护天使）
 *
 * 如果把所有装备名称塞在攻略表的一个字段里，查询和展示都不方便，
 * 所以用一张独立的关联表来存储，每条记录代表一个装备推荐。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<StrategyItem> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法 —— 最常用的操作】
 * - selectById(id)              → 根据 ID 查询单条装备推荐记录
 * - selectList(wrapper)         → 根据条件查询装备推荐列表
 *   ★ 最常用：按 strategyId 查询某个攻略的所有推荐装备
 *
 * 【新增方法】
 * - insert(strategyItem)        → 插入一条装备推荐记录
 *
 * 【修改方法】
 * - updateById(strategyItem)    → 根据 ID 更新装备推荐信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除单条装备推荐
 * - delete(wrapper)             → 根据条件删除（如删除某个攻略的所有装备推荐）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与其他 Mapper 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyItem 通过 strategyId 字段关联到 Strategy 表：
 *
 *   tb_strategy（攻略表）        tb_strategy_item（装备关联表）
 *   ┌────┬──────────┐           ┌────┬────────────┬──────────┬────────┐
 *   │ id │ title    │           │ id │strategyId  │itemName  │category│
 *   ├────┼──────────┤           ├────┼────────────┼──────────┼────────┤
 *   │  1 │盖伦出装  │◄──────────│  1 │  1         │多兰盾    │起始    │
 *   │    │          │◄──────────│  2 │  1         │黑色切割者│核心    │
 *   │    │          │◄──────────│  3 │  1         │守护天使  │可选    │
 *   └────┴──────────┘           └────┴────────────┴──────────┴────────┘
 *
 * 查询流程：
 * 1. StrategyService 查询攻略详情时，先通过 StrategyMapper 查询攻略基本信息
 * 2. 再通过 StrategyItemMapper 按 strategyId 查询该攻略的所有推荐装备
 * 3. 将装备列表组装到 StrategyDetailVO.items 中返回给前端
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
 * 数据库 tb_strategy_item 表
 *     ↕（MyBatis-Plus 自动映射）
 * StrategyItemMapper（本接口）
 *     ↕（StrategyService 调用 Mapper 方法）
 * StrategyService / StrategyServiceImpl
 *     ↕（组装到 StrategyDetailVO.items 中）
 * StrategyController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（攻略详情页展示推荐装备）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - StrategyItem：攻略-装备关联实体类，对应 tb_strategy_item 表
 * - Strategy：攻略实体类，装备通过 strategyId 关联到攻略
 * - StrategyService / StrategyServiceImpl：攻略业务逻辑层，查询攻略详情时会同时查询装备
 * - BaseMapper<StrategyItem>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询某个攻略的所有推荐装备（最常用）
 *   LambdaQueryWrapper<StrategyItem> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(StrategyItem::getStrategyId, strategyId)
 *          .orderByAsc(StrategyItem::getSortOrder);  // 按排序字段升序
 *   List<StrategyItem> items = strategyItemMapper.selectList(wrapper);
 *
 *   // 2. 发布攻略时批量插入装备推荐
 *   for (StrategyItem item : itemList) {
 *       item.setStrategyId(strategyId);  // 设置关联的攻略 ID
 *       strategyItemMapper.insert(item);
 *   }
 *
 *   // 3. 更新攻略时先删除旧装备，再插入新装备
 *   wrapper.eq(StrategyItem::getStrategyId, strategyId);
 *   strategyItemMapper.delete(wrapper);  // 删除旧装备
 *   // 然后插入新装备...
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface StrategyItemMapper extends BaseMapper<StrategyItem> {
    // 当前不需要自定义方法，BaseMapper<StrategyItem> 已提供所有基础 CRUD 操作
    // 最常用的操作是按 strategyId 查询装备列表，通过 LambdaQueryWrapper 即可实现
}
