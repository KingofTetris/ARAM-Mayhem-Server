package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.HeroModifier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 英雄属性修正器数据访问接口（Mapper） —— 修正器数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"英雄属性修正器"数据和数据库之间的桥梁。
 * 当 Service 层需要查询英雄的属性修正数据时，调用这个接口的方法。
 *
 * 什么是属性修正器？
 * ARAM 模式下，部分英雄的属性会被调整（增强或削弱），以保持游戏平衡。
 * 比如：
 * - 某个法师的伤害被降低到 90%（modifierValue = 0.90）
 * - 某个坦克的生命值增加了 10%（modifierValue = 1.10）
 * - 某个刺客的技能急速增加了 20（modifierValue = 1.20）
 *
 * 这些修正信息就存储在 tb_hero_modifier 表中，通过 heroId 关联到对应的英雄。
 * 一个英雄可以有多个修正器（比如同时有伤害修正和生命值修正）。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<HeroModifier> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法 —— 最常用的操作】
 * - selectById(id)              → 根据 ID 查询单个修正器
 * - selectList(wrapper)         → 根据条件查询修正器列表
 *   ★ 最常用：按 heroId 查询某个英雄的所有修正器
 *
 * 【新增方法】
 * - insert(heroModifier)        → 插入一条修正器记录
 *
 * 【修改方法】
 * - updateById(heroModifier)    → 根据 ID 更新修正器信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除修正器
 * - delete(wrapper)             → 根据条件删除（如删除某个英雄的所有修正器）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与其他 Mapper 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroModifier 通过 heroId 字段关联到 Hero 表：
 *
 *   tb_hero（英雄表）          tb_hero_modifier（修正器表）
 *   ┌────┬──────┐             ┌────┬──────┬───────────┬───────────────┐
 *   │ id │ name │             │ id │heroId│modName    │modValue       │
 *   ├────┼──────┤             ├────┼──────┼───────────┼───────────────┤
 *   │  1 │ 提莫 │◄────────────│  1 │  1   │伤害倍率   │0.85           │
 *   │    │      │◄────────────│  2 │  1   │生命值加成 │1.10           │
 *   └────┴──────┘             └────┴──────┴───────────┴───────────────┘
 *
 * 查询流程：
 * 1. HeroService 查询英雄详情时，先通过 HeroMapper 查询英雄基本信息
 * 2. 再通过 HeroModifierMapper 按 heroId 查询该英雄的所有修正器
 * 3. 将修正器列表组装到 HeroDetailVO.modifiers 中返回给前端
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
 * 数据库 tb_hero_modifier 表
 *     ↕（MyBatis-Plus 自动映射）
 * HeroModifierMapper（本接口）
 *     ↕（HeroService 调用 Mapper 方法）
 * HeroService / HeroServiceImpl
 *     ↕（组装到 HeroDetailVO.modifiers 中）
 * HeroController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（英雄详情页展示修正器信息）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - HeroModifier：修正器实体类，对应 tb_hero_modifier 表
 * - Hero：英雄实体类，修正器通过 heroId 关联到英雄
 * - HeroService / HeroServiceImpl：英雄业务逻辑层，查询英雄详情时会同时查询修正器
 * - BaseMapper<HeroModifier>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询某个英雄的所有修正器（最常用）
 *   LambdaQueryWrapper<HeroModifier> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(HeroModifier::getHeroId, heroId);  // 按 heroId 过滤
 *   List<HeroModifier> modifiers = heroModifierMapper.selectList(wrapper);
 *
 *   // 2. 批量插入修正器（数据同步时使用）
 *   for (HeroModifier modifier : modifierList) {
 *       heroModifierMapper.insert(modifier);
 *   }
 *
 *   // 3. 删除某个英雄的所有修正器（更新前先清除旧数据）
 *   wrapper.eq(HeroModifier::getHeroId, heroId);
 *   heroModifierMapper.delete(wrapper);
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface HeroModifierMapper extends BaseMapper<HeroModifier> {
    // 当前不需要自定义方法，BaseMapper<HeroModifier> 已提供所有基础 CRUD 操作
    // 最常用的操作是按 heroId 查询修正器列表，通过 LambdaQueryWrapper 即可实现
}
