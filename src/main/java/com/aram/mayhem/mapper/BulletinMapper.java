package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Bulletin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告数据访问接口（Mapper） —— 公告数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"公告"数据和数据库之间的桥梁。
 * 当 Service 层需要查询、新增、修改或删除公告数据时，调用这个接口的方法。
 *
 * 公告是管理员发布的系统消息，用于通知用户：
 * - 版本更新：如 "v2.0 新增海克斯强化符文推荐"
 * - 活动信息：如 "五一限时双倍经验"
 * - 系统通知：如 "服务器维护通知"
 *
 * 公告可以置顶显示（pinned = 1），让重要公告始终排在列表最前面。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<Bulletin> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法】
 * - selectById(id)              → 根据 ID 查询单个公告（用于查看公告详情）
 * - selectList(wrapper)         → 根据条件查询公告列表
 *   ★ 最常用：查询所有公告，按置顶+创建时间排序
 * - selectPage(page, wrapper)   → 分页查询公告列表
 * - selectCount(wrapper)        → 统计符合条件的公告数量
 *
 * 【新增方法】
 * - insert(bulletin)            → 插入一条公告记录（管理员发布新公告）
 *
 * 【修改方法】
 * - updateById(bulletin)        → 根据 ID 更新公告信息（编辑公告内容）
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除公告（逻辑删除，设置 deleted=1）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告列表的排序规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告列表的排序逻辑（在 Service 层实现）：
 * 1. 置顶公告排在最前面（pinned = 1 的公告）
 * 2. 非置顶公告按创建时间倒序排列（最新的在前）
 *
 * 使用 LambdaQueryWrapper 实现：
 *   wrapper.orderByDesc(Bulletin::getPinned)    // 置顶的排前面
 *          .orderByDesc(Bulletin::getCreatedAt); // 新的排前面
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、初始数据
 * ═══════════════════════════════════════════════════════════════════
 *
 * 系统启动时，BulletinDataInitializer 会自动插入初始公告数据，
 * 确保公告页面不会显示空白。初始公告包括：
 * - 版本更新公告
 * - 欢迎公告
 * - 使用指南公告
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
 * 数据库 tb_bulletin 表
 *     ↕（MyBatis-Plus 自动映射，逻辑删除自动过滤）
 * BulletinMapper（本接口）
 *     ↕（BulletinService 调用 Mapper 方法）
 * BulletinService / BulletinServiceImpl
 *     ↕（Controller 调用 Service 方法）
 * BulletinController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（公告列表页、公告详情页）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Bulletin：公告实体类，对应 tb_bulletin 表
 * - BulletinService / BulletinServiceImpl：公告的业务逻辑层
 * - BulletinController：公告的 API 接口层
 * - BulletinDataInitializer：启动时插入初始公告数据
 * - BaseMapper<Bulletin>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 八、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 查询所有公告（置顶在前，时间倒序）
 *   LambdaQueryWrapper<Bulletin> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.orderByDesc(Bulletin::getPinned)
 *          .orderByDesc(Bulletin::getCreatedAt);
 *   List<Bulletin> bulletins = bulletinMapper.selectList(wrapper);
 *
 *   // 2. 分页查询公告列表
 *   Page<Bulletin> page = new Page<>(1, 10);
 *   Page<Bulletin> result = bulletinMapper.selectPage(page, wrapper);
 *
 *   // 3. 查看公告详情
 *   Bulletin bulletin = bulletinMapper.selectById(bulletinId);
 *
 *   // 4. 发布新公告（管理员操作）
 *   Bulletin bulletin = new Bulletin();
 *   bulletin.setType("version");
 *   bulletin.setTitle("v2.0 版本更新");
 *   bulletin.setContent("新增海克斯强化符文推荐功能...");
 *   bulletin.setPinned(1);  // 置顶
 *   bulletinMapper.insert(bulletin);
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface BulletinMapper extends BaseMapper<Bulletin> {
    // 当前不需要自定义方法，BaseMapper<Bulletin> 已提供所有基础 CRUD 操作
    // 最常用的操作是查询公告列表（按置顶+时间排序），通过 LambdaQueryWrapper 即可实现
}
