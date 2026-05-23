package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口（Mapper） —— 用户数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"用户"数据和数据库之间的桥梁。
 * 当 Service 层需要查询、新增、修改或删除用户数据时，调用这个接口的方法。
 *
 * 用户数据是整个系统的核心之一，涉及：
 * - 注册：创建新用户记录
 * - 登录：根据邮箱查询用户，验证密码
 * - 个人资料：查看和修改用户信息
 * - 权限控制：根据用户角色决定能做什么操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、继承 BaseMapper<User> 后自动获得的方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【查询方法 —— 认证场景最常用】
 * - selectById(id)              → 根据 ID 查询用户（用于获取当前登录用户信息）
 * - selectOne(wrapper)          → 根据条件查询单个用户
 *   ★ 最常用：按 email 查询用户（登录时使用）
 * - selectList(wrapper)         → 根据条件查询用户列表
 * - selectCount(wrapper)        → 统计符合条件的用户数量
 *   ★ 用于注册时检查邮箱是否已被注册
 *
 * 【新增方法】
 * - insert(user)                → 插入一条用户记录（注册时使用）
 *
 * 【修改方法】
 * - updateById(user)            → 根据 ID 更新用户信息（修改个人资料时使用）
 * - update(user, wrapper)       → 根据条件更新用户信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除用户（逻辑删除，设置 deleted=1）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、安全相关说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 用户表包含敏感信息（密码），操作时需要注意：
 *
 * 1. 密码字段安全：
 *    - 数据库中存储的是 BCrypt 加密后的密码，不是明文
 *    - 查询时不会返回密码字段（DTO 层过滤）
 *    - 密码验证使用 BCrypt.checkpw()，不是直接比较字符串
 *
 * 2. 邮箱唯一性：
 *    - 注册时需要检查邮箱是否已存在
 *    - 使用 selectCount + email 条件判断
 *
 * 3. 逻辑删除：
 *    - User 实体使用 @TableLogic 注解
 *    - 删除用户不会真正删除记录，而是设置 deleted=1
 *    - 查询时自动过滤已删除的用户
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、与其他 Mapper 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * User 是系统的核心实体，被多个模块引用：
 *
 *   tb_user（用户表）
 *       │
 *       ├── tb_strategy（攻略表）→ 通过 userId 关联，用户发布的攻略
 *       │
 *       ├── tb_vote（投票表）→ 通过 userId 关联，用户的投票记录
 *       │
 *       └── Spring Security → CustomUserDetailsService 加载用户信息进行认证
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
 * 数据库 tb_user 表
 *     ↕（MyBatis-Plus 自动映射，逻辑删除自动过滤）
 * UserMapper（本接口）
 *     ↕（多个 Service 调用 Mapper 方法）
 * ├── AuthService / AuthServiceImpl（注册/登录/刷新Token）
 * ├── CustomUserDetailsService（Spring Security 加载用户信息）
 * ├── StrategyService（查询攻略作者信息）
 * └── UserController（获取/更新用户资料）
 *     ↕（Controller 调用 Service 方法）
 * AuthController / UserController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端（注册/登录/个人资料页）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - User：用户实体类，对应 tb_user 表
 * - AuthService / AuthServiceImpl：认证业务逻辑层，注册和登录时使用
 * - CustomUserDetailsService：Spring Security 的用户加载服务
 * - AuthController：认证 API 接口层
 * - UserController：用户资料 API 接口层
 * - BaseMapper<User>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 八、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 根据邮箱查询用户（登录时使用）
 *   LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(User::getEmail, email);
 *   User user = userMapper.selectOne(wrapper);
 *
 *   // 2. 检查邮箱是否已被注册
 *   wrapper.eq(User::getEmail, email);
 *   Long count = userMapper.selectCount(wrapper);
 *   if (count > 0) {
 *       throw new BusinessException("邮箱已被注册");
 *   }
 *
 *   // 3. 注册新用户
 *   User newUser = new User();
 *   newUser.setEmail("test@example.com");
 *   newUser.setPassword(passwordEncoder.encode("123456"));  // BCrypt 加密
 *   newUser.setNickname("新用户");
 *   userMapper.insert(newUser);
 *
 *   // 4. 更新用户资料
 *   user.setNickname("新昵称");
 *   user.setDisplayMode(1);  // 切换为深色模式
 *   userMapper.updateById(user);
 *
 *   // 5. Spring Security 加载用户（在 CustomUserDetailsService 中）
 *   User user = userMapper.selectOne(
 *       new LambdaQueryWrapper<User>().eq(User::getEmail, email)
 *   );
 *   // 转换为 Spring Security 的 UserDetails 对象
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface UserMapper extends BaseMapper<User> {
    // 当前不需要自定义方法，BaseMapper<User> 已提供所有基础 CRUD 操作
    // 最常用的操作是按 email 查询用户（登录）和按 ID 查询用户（获取资料），
    // 都可以通过 LambdaQueryWrapper 实现
}
