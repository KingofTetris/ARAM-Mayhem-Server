package com.aram.mayhem.service.impl;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.UpdateProfileRequest;
import com.aram.mayhem.dto.UserProfileVO;
import com.aram.mayhem.entity.Strategy;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.StrategyMapper;
import com.aram.mayhem.mapper.UserMapper;
import com.aram.mayhem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类 —— 个人中心模块的"后厨"，真正做菜的地方
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 UserService 接口，是个人中心模块所有业务逻辑的"真正执行者"。
 * UserService 接口只定义了"有哪些方法"（菜单），这个类负责"具体怎么做"（做菜）。
 *
 * 个人中心模块允许用户：
 * - 查看自己的个人资料（昵称、头像、邮箱等）
 * - 修改自己的个人资料（昵称、头像、显示模式、通知开关）
 * - 查看自己发布的攻略数量统计
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * UserMapper            | 操作 tb_user 数据库表          | 仓库管理员（取用户数据）
 * StrategyMapper        | 操作 tb_strategy 数据库表      | 仓库管理员（统计攻略数量）
 * Logger (log)          | 记录运行日志                   | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、部分更新策略说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类的 updateProfile() 方法采用"部分更新"策略（Partial Update）：
 * - 只更新请求中非 null 的字段
 * - 如果某个字段为 null，表示用户不想修改这个字段，保持原值不变
 *
 * 这与"全量更新"不同：
 * - 全量更新：所有字段都会被覆盖，null 也会覆盖原有值
 * - 部分更新：只更新用户明确提供的字段
 *
 * 举例：
 * 用户只想修改昵称，请求体为 {"nickname": "新昵称"}
 * → 只有 nickname 被更新，avatarUrl、displayMode 等保持不变
 *
 * 如果是全量更新，avatarUrl 等未提供的字段会被设为 null，导致数据丢失！
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、异常处理说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类使用 BusinessException（自定义业务异常）来处理用户不存在的情况：
 * - 抛出 new BusinessException(404, "User not found with id: xxx")
 * - 全局异常处理器会捕获这个异常，返回 HTTP 404 响应
 *
 * 为什么不用 IllegalArgumentException？
 * - IllegalArgumentException 是 Java 标准异常，语义不够精确
 * - BusinessException 可以携带 HTTP 状态码，方便全局异常处理器返回正确的 HTTP 响应
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                        | 是否事务
 * ------------------------|----------------------------|----------
 * getUserProfile()        | 查询用户资料（含攻略统计）  | 否
 * updateProfile()         | 部分更新用户资料            | 否
 * convertToProfileVO()    | 实体→VO转换（私有）         | 否
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * 日志记录器 —— 用来记录程序运行过程中的关键信息
     *
     * 本类主要使用 INFO 和 WARN 级别：
     * - INFO：记录正常的查询和更新操作
     * - WARN：记录用户不存在的异常情况
     */
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * 用户数据访问对象 —— 负责与 tb_user 表交互
     *
     * 主要操作：
     * - selectById()     → 根据ID查询用户
     * - updateById()     → 根据ID更新用户
     *
     * 对应数据库表：tb_user
     * 主要字段：id, email, password, nickname, avatar_url, display_mode, notification_enabled, role
     */
    private final UserMapper userMapper;

    /**
     * 攻略数据访问对象 —— 用于统计用户发布的攻略数量
     *
     * 在本类中的用途：
     * - selectCount() → 统计指定用户发布的攻略总数
     * - 这个统计数据会显示在个人中心页面
     *
     * 对应数据库表：tb_strategy
     */
    private final StrategyMapper strategyMapper;

    /**
     * 构造函数 —— Spring 自动注入所有依赖
     *
     * @param userMapper      用户数据访问对象
     * @param strategyMapper  攻略数据访问对象（用于统计攻略数量）
     */
    public UserServiceImpl(UserMapper userMapper, StrategyMapper strategyMapper) {
        this.userMapper = userMapper;
        this.strategyMapper = strategyMapper;
    }

    /**
     * 获取用户资料 —— 个人中心的核心查询功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户打开个人中心页面时，需要展示该用户的完整资料。
     * 这个方法根据用户ID查询用户信息，并附加攻略数量统计。
     *
     * 返回的信息包括：
     * - 基本信息：ID、邮箱、昵称、头像
     * - 偏好设置：显示模式、通知开关
     * - 角色信息：用户角色（USER/ADMIN）
     * - 统计数据：发布的攻略数量、收藏数量
     *
     * @param userId 用户ID（从JWT Token中获取）
     * @return UserProfileVO 用户资料视图对象
     *
     * @throws BusinessException 当用户不存在时抛出（HTTP 404）
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 根据ID查询用户 → 不存在则抛出 404 异常
     * 2. 统计用户发布的攻略数量
     * 3. 转换为 UserProfileVO 返回
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要统计攻略数量？
     * ══════════════════════════════════════════════════════════════
     *
     * 个人中心页面需要展示"我发布了多少篇攻略"这样的统计信息。
     * 攻略数量不是 tb_user 表的字段，而是需要从 tb_strategy 表中 COUNT 计算得出。
     * 所以需要额外调用 strategyMapper.selectCount() 来获取这个统计数据。
     */
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        // ─── 记录操作日志 ───
        log.info("Getting user profile: userId={}", userId);

        // ─── 根据ID查询用户 ───
        // selectById() 生成 SQL：SELECT * FROM tb_user WHERE id = ?
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 用户不存在，记录警告日志并抛出业务异常
            // 抛出404异常后，全局异常处理器会返回 HTTP 404 响应给前端
            log.warn("User not found: userId={}", userId);
            throw new BusinessException(404, "User not found with id: " + userId);
        }

        // ─── 统计用户发布的攻略数量 ───
        // selectCount() 生成 SQL：SELECT COUNT(*) FROM tb_strategy WHERE user_id = ?
        // LambdaQueryWrapper 构建查询条件：只统计该用户发布的攻略
        Long strategyCount = strategyMapper.selectCount(
                new LambdaQueryWrapper<Strategy>().eq(Strategy::getUserId, userId)
        );

        // ─── 转换为VO返回 ───
        // convertToProfileVO() 将 User 实体和攻略数量组合成 UserProfileVO
        return convertToProfileVO(user, strategyCount.intValue());
    }

    /**
     * 更新用户资料 —— 个人中心的资料修改功能
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 当用户在个人中心修改自己的资料时，前端会调用这个方法。
     * 它采用"部分更新"策略：只更新请求中非 null 的字段。
     *
     * ══════════════════════════════════════════════════════════════
     * 可更新的字段
     * ══════════════════════════════════════════════════════════════
     *
     * 字段名                  | 类型      | 说明
     * ────────────────────────────────────────────────────────
     * nickname                | String   | 用户昵称
     * avatarUrl               | String   | 头像URL
     * displayMode             | String   | 显示模式（如"dark"/"light"）
     * notificationEnabled     | Boolean  | 是否开启通知
     *
     * 不可更新的字段（出于安全考虑）：
     * - id：用户ID不可修改
     * - email：邮箱修改需要单独的验证流程
     * - password：密码修改需要单独的修改密码功能
     * - role：角色不可自行修改（防止提权）
     *
     * @param userId  用户ID（从JWT Token中获取，不是前端传的）
     * @param request 更新请求体，包含要更新的字段
     * @return UserProfileVO 更新后的用户资料
     *
     * @throws BusinessException 当用户不存在时抛出（HTTP 404）
     *
     * ══════════════════════════════════════════════════════════════
     * 部分更新的实现原理
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 先从数据库查出完整的 User 对象（包含所有当前值）
     * 2. 逐个检查 request 中的字段是否为 null
     * 3. 非 null 的字段才覆盖 User 对象的对应属性
     * 4. 将修改后的 User 对象整体更新回数据库
     *
     * 这样做的好处：
     * - 前端只需要发送要修改的字段，不需要发送全部字段
     * - 避免了"未提供的字段被覆盖为null"的问题
     * - 减少了网络传输的数据量
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么不需要 @Transactional？
     * ══════════════════════════════════════════════════════════════
     *
     * 这个方法只涉及一次数据库写操作（updateById），
     * 不存在多步操作需要原子性保证的情况，
     * 所以不需要 @Transactional 注解。
     */
    @Override
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        // ─── 记录操作日志 ───
        log.info("Updating user profile: userId={}, nickname={}", userId, request.getNickname());

        // ─── 根据ID查询用户 ───
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("User not found for update: userId={}", userId);
            throw new BusinessException(404, "User not found with id: " + userId);
        }

        // ─── 部分更新：只更新非 null 的字段 ───
        // 每个字段单独判断，如果 request 中提供了新值（非null），才覆盖原有值
        // 如果 request 中没有提供（null），保持原有值不变

        // 昵称：用户想修改昵称
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }

        // 头像URL：用户更换了头像
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // 显示模式：用户切换了深色/浅色模式
        if (request.getDisplayMode() != null) {
            user.setDisplayMode(request.getDisplayMode());
        }

        // 通知开关：用户开启或关闭了通知
        if (request.getNotificationEnabled() != null) {
            user.setNotificationEnabled(request.getNotificationEnabled());
        }

        // ─── 更新到数据库 ───
        // updateById() 生成 SQL：UPDATE tb_user SET nickname=?, avatar_url=?, ... WHERE id=?
        // MyBatis-Plus 默认只更新非 null 的字段（取决于全局配置）
        userMapper.updateById(user);
        log.info("User profile updated: userId={}", userId);

        // ─── 重新统计攻略数量 ───
        // 更新后返回完整的用户资料，需要包含最新的攻略数量
        Long strategyCount = strategyMapper.selectCount(
                new LambdaQueryWrapper<Strategy>().eq(Strategy::getUserId, userId)
        );

        // ─── 返回更新后的用户资料 ───
        return convertToProfileVO(user, strategyCount.intValue());
    }

    /**
     * 实体转VO —— 将 User 实体转换为 UserProfileVO
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 这是一个私有辅助方法，用于将数据库查询出来的 User 实体对象
     * 转换为前端需要的 UserProfileVO（View Object，视图对象）。
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要转换？
     * ══════════════════════════════════════════════════════════════
     *
     * User 实体包含 password 字段，这是敏感信息，不应该返回给前端。
     * UserProfileVO 过滤掉了 password 字段，只包含前端需要展示的信息。
     *
     * 此外，UserProfileVO 还包含 User 实体中没有的统计字段：
     * - strategyCount：发布的攻略数量（需要额外查询）
     * - favoriteCount：收藏数量（目前固定为0，预留字段）
     *
     * ══════════════════════════════════════════════════════════════
     * VO 与实体的字段对应关系
     * ══════════════════════════════════════════════════════════════
     *
     * User 实体字段                  → UserProfileVO 字段       | 说明
     * ─────────────────────────────────────────────────────────────────
     * id                            → id                      | 直接复制
     * email                         → email                   | 直接复制
     * nickname                      → nickname                | 直接复制
     * avatarUrl                     → avatarUrl               | 直接复制
     * displayMode                   → displayMode             | 直接复制
     * notificationEnabled           → notificationEnabled     | 直接复制
     * role                          → role                    | 直接复制
     * password                      → （不包含）               | 敏感信息，不返回
     * strategyCount（额外查询）      → strategyCount           | 攻略数量
     * 0（固定值）                    → favoriteCount           | 收藏数量（预留）
     *
     * ══════════════════════════════════════════════════════════════
     * Builder 模式说明
     * ══════════════════════════════════════════════════════════════
     *
     * UserProfileVO.builder() 使用了 Lombok 的 @Builder 注解生成的建造者模式。
     * 好处：
     * - 代码更清晰：每个字段赋值都有明确的标识
     * - 可读性更强：一眼就能看出哪个值赋给了哪个字段
     * - 不需要记住构造函数参数的顺序
     *
     * 对比传统方式：
     * 传统：new UserProfileVO(id, email, nickname, ...)  ← 参数顺序容易搞混
     * Builder：UserProfileVO.builder().id(x).email(y).nickname(z)...build()  ← 一目了然
     *
     * @param user          用户实体对象（来自数据库查询）
     * @param strategyCount 攻略数量（从 tb_strategy 表 COUNT 得出）
     * @return UserProfileVO 用户资料视图对象（给前端使用）
     */
    private UserProfileVO convertToProfileVO(User user, int strategyCount) {
        return UserProfileVO.builder()
                .id(user.getId())                                // 用户ID
                .email(user.getEmail())                          // 邮箱
                .nickname(user.getNickname())                    // 昵称
                .avatarUrl(user.getAvatarUrl())                  // 头像URL
                .displayMode(user.getDisplayMode())              // 显示模式（dark/light）
                .notificationEnabled(user.getNotificationEnabled()) // 通知开关
                .role(user.getRole())                            // 用户角色（USER/ADMIN）
                .strategyCount(strategyCount)                    // 发布的攻略数量
                .favoriteCount(0)                                // 收藏数量（目前固定为0，预留字段）
                .build();                                        // 构建最终对象
    }
}
