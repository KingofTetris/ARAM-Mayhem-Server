package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Augment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 强化符文数据访问接口（Mapper） —— 符文数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"强化符文"数据和数据库之间的桥梁。
 * 当 Service 层需要查询、新增、修改或删除符文数据时，调用这个接口的方法。
 *
 * 强化符文（Augment）是 ARAM 模式中的特殊增益效果，每局游戏开始时玩家可以从中选择。
 * 符文数据包括：名称、描述、品质（银/金/棱彩）、所属套装、胜率、选取率等。
 *
 * 打个比方：
 * - 数据库 = 仓库（存放所有符文数据）
 * - AugmentMapper = 仓库管理员（负责从仓库取东西、放东西）
 * - AugmentService = 部门经理（告诉管理员需要什么，管理员去执行）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<Augment> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法】
 * - selectById(id)              → 根据 ID 查询单个符文
 * - selectBatchIds(idList)      → 根据 ID 列表批量查询多个符文
 * - selectOne(wrapper)          → 根据条件查询单个符文（如按 nameEn 查询）
 * - selectList(wrapper)         → 根据条件查询符文列表（如按品质查询所有棱彩符文）
 * - selectPage(page, wrapper)   → 分页查询符文列表（用于符文列表页）
 * - selectCount(wrapper)        → 统计符合条件的符文数量
 *
 * 【新增方法】
 * - insert(augment)             → 插入一条符文记录
 *
 * 【修改方法】
 * - updateById(augment)         → 根据 ID 更新符文信息
 * - update(augment, wrapper)    → 根据条件更新符文信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除符文
 * - deleteBatchIds(idList)      → 根据 ID 列表批量删除
 * - delete(wrapper)             → 根据条件删除
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文数据的特殊之处
 * ═══════════════════════════════════════════════════════════════════
 *
 * Augment 实体类中有 JSON 类型的字段（如 topHeroes），需要特殊处理：
 * - Entity 类上使用 @TableName(value = "tb_augment", autoResultMap = true)
 * - autoResultMap = true 让 MyBatis-Plus 自动处理 TypeHandler
 * - 这样 JSON 字段在读取时自动反序列化为 Java List，写入时自动序列化为 JSON
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
 * 数据库 tb_augment 表
 *     ↕（MyBatis-Plus 自动映射，JSON 字段通过 TypeHandler 处理）
 * AugmentMapper（本接口）
 *     ↕（Service 调用 Mapper 方法）
 * AugmentService / AugmentServiceImpl
 *     ↕（Controller 调用 Service 方法）
 * AugmentController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Augment：符文实体类，对应 tb_augment 表
 * - AugmentService / AugmentServiceImpl：符文的业务逻辑层
 * - AugmentController：符文的 API 接口层
 * - StrategyAugment：攻略-符文关联表，通过 augmentId 关联到 Augment
 * - BaseMapper<Augment>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 根据 ID 查询符文
 *   Augment augment = augmentMapper.selectById(1L);
 *
 *   // 2. 按品质查询所有棱彩符文
 *   LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(Augment::getQuality, "Prismatic")
 *          .orderByDesc(Augment::getWinRate);
 *   List<Augment> prismaticAugments = augmentMapper.selectList(wrapper);
 *
 *   // 3. 按套装查询符文（用于套装追踪功能）
 *   wrapper.eq(Augment::getSynergySet, "精密")
 *          .or()
 *          .eq(Augment::getSynergySet2, "精密")
 *          .or()
 *          .eq(Augment::getSynergySet3, "精密");
 *   List<Augment> synergyAugments = augmentMapper.selectList(wrapper);
 *
 *   // 4. 分页查询符文列表
 *   Page<Augment> page = new Page<>(1, 20);
 *   Page<Augment> result = augmentMapper.selectPage(page, wrapper);
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface AugmentMapper extends BaseMapper<Augment> {
    // 当前不需要自定义方法，BaseMapper<Augment> 已提供所有基础 CRUD 操作
    // 如果未来需要复杂的自定义查询（如按套装名模糊搜索），可以在这里添加方法声明
}
