package com.aram.mayhem.service;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.AuthResponse;
import com.aram.mayhem.dto.LoginRequest;
import com.aram.mayhem.dto.RefreshTokenRequest;
import com.aram.mayhem.dto.RegisterRequest;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.UserMapper;
import com.aram.mayhem.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务类 —— 系统的"门卫"，负责验证用户身份并发放通行证
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是整个系统的"安全大门"，负责三件核心事情：
 * 1. register()     → 用户注册（创建新账号）
 * 2. login()        → 用户登录（验证身份）
 * 3. refreshToken() → 刷新令牌（续期通行证）
 *
 * 打个比方：
 * - 注册 = 去酒店前台办理入住，拿到房卡（Access Token）和预订确认信（Refresh Token）
 * - 登录 = 已有预订的客人回到酒店，用身份证验证后拿到房卡
 * - 刷新令牌 = 房卡过期了，拿预订确认信去前台换一张新房卡，不用重新办理入住
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、认证流程全景图
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【注册流程】
 * ┌──────────┐    ┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
 * │ 前端      │    │ AuthService      │    │ 数据库       │    │ JwtToken     │
 * │ 注册页面  │──→│ register()       │──→│ tb_user 表   │    │ Provider     │
 * └──────────┘    │ ①检查邮箱唯一性  │    │ 插入用户记录 │    │              │
 *                 │ ②加密密码(BCrypt) │    └──────────────┘    │              │
 *                 │ ③设置默认值       │                        │              │
 *                 │ ④插入用户记录     │───────────────────────→│ 生成Token对  │
 *                 │ ⑤生成Token对      │←───────────────────────│              │
 *                 └──────────────────┘                        └──────────────┘
 *
 * 【登录流程】
 * ┌──────────┐    ┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
 * │ 前端      │    │ AuthService      │    │ 数据库       │    │ JwtToken     │
 * │ 登录页面  │──→│ login()          │──→│ tb_user 表   │    │ Provider     │
 * └──────────┘    │ ①查询用户        │    │ 查询用户记录 │    │              │
 *                 │ ②验证密码(BCrypt) │    └──────────────┘    │              │
 *                 │ ③生成Token对      │───────────────────────→│ 生成Token对  │
 *                 └──────────────────┘                        └──────────────┘
 *
 * 【刷新令牌流程】
 * ┌──────────┐    ┌──────────────────────┐    ┌──────────────┐    ┌──────────────┐
 * │ 前端      │    │ AuthService          │    │ 数据库       │    │ JwtToken     │
 * │ API拦截器 │──→│ refreshToken()       │──→│ tb_user 表   │    │ Provider     │
 * └──────────┘    │ ①验证Token签名+有效期 │    │ 验证用户存在 │    │              │
 *                 │ ②校验Token类型=refresh │    └──────────────┘    │              │
 *                 │ ③提取用户ID和邮箱     │                        │              │
 *                 │ ④验证用户是否存在     │                        │              │
 *                 │ ⑤生成新Token对        │───────────────────────→│ 生成新Token  │
 *                 └──────────────────────┘                        └──────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                               | 打个比方
 * ----------------------|-----------------------------------|------------------
 * UserMapper            | 操作 tb_user 数据库表              | 仓库管理员（取/存用户数据）
 * PasswordEncoder       | BCrypt 密码加密和验证              | 保险柜（加密存密码，验证密码）
 * JwtTokenProvider      | 生成和解析 JWT Token               | 印章处（发通行证，验通行证）
 * Logger (log)          | 记录运行日志                       | 工作记录本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、安全设计要点
 * ═══════════════════════════════════════════════════════════════════
 *
 * 【密码安全】
 * - 密码永远不以明文存储，使用 BCrypt 算法加密后存入数据库
 * - BCrypt 的特点：每次加密同一密码，结果都不同（自带随机盐值）
 * - 验证密码时，使用 passwordEncoder.matches(明文, 密文) 方法
 * - 即使数据库泄露，攻击者也无法还原出原始密码
 *
 * 【Token 安全】
 * - Access Token（访问令牌）：短期有效（2小时），用于日常 API 访问
 * - Refresh Token（刷新令牌）：长期有效（7天），仅用于刷新 Access Token
 * - 两种 Token 都包含 type 标记（"access" 或 "refresh"），防止混用
 * - 刷新令牌时会校验 type=refresh，防止 Access Token 被用来刷新
 *
 * 【错误信息安全】
 * - 登录失败时，错误信息统一为 "Invalid email or password"
 * - 不告诉用户"邮箱不存在"还是"密码错误"，防止攻击者枚举用户
 * - 注册时邮箱重复是唯一允许暴露具体原因的场景（用户自己知道自己的邮箱）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、与前端的数据交互
 * ═══════════════════════════════════════════════════════════════════
 *
 * 注册请求：POST /api/auth/register
 *   请求体：{ "email": "user@example.com", "password": "123456", "nickname": "玩家" }
 *   响应体：{ "userId": 1, "email": "...", "nickname": "...", "role": "USER",
 *             "accessToken": "xxx.yyy.zzz", "refreshToken": "aaa.bbb.ccc" }
 *
 * 登录请求：POST /api/auth/login
 *   请求体：{ "email": "user@example.com", "password": "123456" }
 *   响应体：同注册响应
 *
 * 刷新令牌请求：POST /api/auth/refresh
 *   请求体：{ "refreshToken": "aaa.bbb.ccc" }
 *   响应体：同注册响应（包含新的 Token 对）
 *
 * 关联类：
 * @see com.aram.mayhem.security.JwtTokenProvider JWT 令牌生成与校验
 * @see com.aram.mayhem.controller.AuthController 认证相关的 REST API 接口
 * @see com.aram.mayhem.dto.AuthResponse 认证成功后的响应对象
 * @see com.aram.mayhem.dto.RegisterRequest 注册请求对象
 * @see com.aram.mayhem.dto.LoginRequest 登录请求对象
 * @see com.aram.mayhem.dto.RefreshTokenRequest 刷新令牌请求对象
 * @see com.aram.mayhem.common.BusinessException 业务异常类
 * @see com.aram.mayhem.entity.User 用户实体类
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 构造函数 —— 通过依赖注入获取所有帮手
     *
     * Spring 会自动找到这三个 Bean 并注入进来：
     * - UserMapper：MyBatis-Plus 自动生成的数据库操作接口
     * - PasswordEncoder：在 SecurityConfig 中配置的 BCryptPasswordEncoder 实例
     * - JwtTokenProvider：JWT 令牌工具类，负责生成和解析 Token
     *
     * 为什么用构造函数注入而不是 @Autowired 字段注入？
     * - 构造函数注入是 Spring 官方推荐的方式
     * - 可以确保依赖不可变（final 字段）
     * - 可以在构造时就能发现依赖缺失的问题
     * - 更容易进行单元测试（可以直接 new 对象传入 mock）
     *
     * @param userMapper         用户数据访问层 —— 操作 tb_user 表的增删改查
     * @param passwordEncoder    密码加密器（BCrypt）── 加密密码和验证密码
     * @param jwtTokenProvider   JWT 令牌生成器 ── 生成和解析 Access/Refresh Token
     */
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户注册 —— 创建新账号并返回认证信息
     *
     * ═══════════════════════════════════════════════════════════════════
     * 这个方法做了什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 注册流程分为 5 步：
     * ① 检查邮箱是否已被注册（邮箱必须唯一）
     * ② 创建 User 实体并设置默认值
     * ③ 使用 BCrypt 加密密码（永远不存明文！）
     * ④ 将用户记录插入数据库
     * ⑤ 生成 Access Token 和 Refresh Token 返回给前端
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么要检查邮箱唯一性？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 邮箱是用户的唯一登录标识，如果两个用户使用同一个邮箱注册，
     * 登录时就无法区分是哪个用户。虽然数据库表也有唯一约束，
     * 但在代码层提前检查可以给出更友好的错误提示。
     *
     * ═══════════════════════════════════════════════════════════════════
     * 新用户的默认值说明
     * ═══════════════════════════════════════════════════════════════════
     *
     * 字段                | 默认值     | 含义
     * --------------------|-----------|----------------------------------
     * role                | "USER"    | 普通用户角色（非管理员）
     * displayMode         | 0         | 浅色模式（1=深色模式）
     * notificationEnabled | 1         | 开启通知（0=关闭通知）
     * avatarUrl           | ""        | 空头像（前端显示默认头像）
     * createdAt           | 自动填充   | 数据库自动设置当前时间
     * updatedAt           | 自动填充   | 数据库自动设置当前时间
     *
     * @param request 注册请求体，包含三个必填字段：
     *                - email：注册邮箱（必须唯一，格式合法）
     *                - password：注册密码（6~64位）
     *                - nickname：用户昵称（2~64位）
     * @return AuthResponse 注册成功后的认证响应，包含：
     *         - userId：新用户的 ID（数据库自增生成）
     *         - email：用户邮箱
     *         - nickname：用户昵称
     *         - role：用户角色（"USER"）
     *         - accessToken：访问令牌（2小时有效）
     *         - refreshToken：刷新令牌（7天有效）
     * @throws BusinessException 当邮箱已被注册时抛出，错误码 409（Conflict）
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("User registration attempt: email={}", request.getEmail());

        // ──── 第①步：检查邮箱是否已被注册 ────
        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建查询条件
        // LambdaQueryWrapper<User>.eq(User::getEmail, request.getEmail())
        //   等价于 SQL：SELECT * FROM tb_user WHERE email = ? AND deleted = 0
        // 注意：MyBatis-Plus 的 @TableLogic 会自动加上 deleted = 0 条件
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );

        // 如果查询结果不为 null，说明这个邮箱已经被注册了
        if (existing != null) {
            log.warn("Registration failed: email already registered, email={}", request.getEmail());
            // 抛出 409 Conflict 错误，告诉前端"邮箱冲突"
            // 前端收到 409 后，会提示用户"该邮箱已注册，请直接登录"
            throw new BusinessException(409, "Email already registered");
        }

        // ──── 第②步：创建 User 实体并设置默认值 ────
        User user = new User();
        user.setEmail(request.getEmail());

        // ──── 第③步：使用 BCrypt 加密密码 ────
        // passwordEncoder.encode() 会：
        // 1. 自动生成一个随机盐值（salt）
        // 2. 将盐值和密码一起进行哈希运算
        // 3. 返回一个 60 字符的字符串，格式如：$2a$10$N9qo8uLOickgx2ZMRZoMye...
        //    - $2a$：BCrypt 算法版本
        //    - $10$：计算轮数（2^10 = 1024 次）
        //    - 后面：盐值 + 哈希结果
        // 每次调用 encode() 对同一个密码，结果都不同（因为盐值随机）
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setNickname(request.getNickname());
        user.setRole("USER");           // 默认普通用户角色（非管理员）
        user.setDisplayMode(0);         // 默认浅色模式（0=浅色, 1=深色）
        user.setNotificationEnabled(1); // 默认开启通知（1=开启, 0=关闭）
        user.setAvatarUrl("");          // 默认空头像，前端会显示默认头像图片

        // ──── 第④步：将用户记录插入数据库 ────
        // userMapper.insert(user) 会执行：
        // INSERT INTO tb_user (email, password, nickname, role, display_mode,
        //                      notification_enabled, avatar_url, created_at, updated_at)
        // VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        // 插入成功后，user.getId() 会自动获取数据库生成的主键 ID
        userMapper.insert(user);
        log.info("User registered successfully: userId={}, email={}", user.getId(), user.getEmail());

        // ──── 第⑤步：生成 Access Token 和 Refresh Token ────
        // Access Token：短期令牌（2小时），用于日常 API 访问
        //   前端每次请求 API 时在 Header 中携带：Authorization: Bearer <accessToken>
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        // Refresh Token：长期令牌（7天），仅用于刷新 Access Token
        //   当 Access Token 过期后，前端用这个 Token 换取新的 Access Token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 构建认证响应对象，使用 Builder 模式
        // Builder 模式的好处：参数多时不会搞错顺序，代码可读性更好
        return AuthResponse.builder()
                .userId(user.getId())          // 用户 ID（数据库自增生成）
                .email(user.getEmail())        // 用户邮箱
                .nickname(user.getNickname())  // 用户昵称
                .role(user.getRole())          // 用户角色（"USER"）
                .accessToken(accessToken)      // 访问令牌
                .refreshToken(refreshToken)    // 刷新令牌
                .build();
    }

    /**
     * 用户登录 —— 验证身份并返回认证信息
     *
     * ═══════════════════════════════════════════════════════════════════
     * 这个方法做了什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 登录流程分为 3 步：
     * ① 根据邮箱查询用户记录
     * ② 验证密码是否正确（BCrypt 匹配）
     * ③ 生成 Access Token 和 Refresh Token 返回给前端
     *
     * ═══════════════════════════════════════════════════════════════════
     * 安全设计：为什么不区分"邮箱不存在"和"密码错误"？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 两种情况都返回相同的错误信息 "Invalid email or password"，
     * 这是为了防止"用户枚举攻击"：
     * - 如果告诉攻击者"邮箱不存在"，攻击者就知道哪些邮箱没注册
     * - 攻击者可以用这个信息来探测系统中有哪些用户
     * - 统一错误信息让攻击者无法判断是邮箱问题还是密码问题
     *
     * ═══════════════════════════════════════════════════════════════════
     * BCrypt 密码验证原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * passwordEncoder.matches(明文密码, 数据库中的哈希值) 的工作过程：
     * 1. 从哈希值中提取盐值（BCrypt 哈希值自带盐值）
     * 2. 用相同的盐值对用户输入的明文密码进行哈希
     * 3. 比较两次哈希结果是否相同
     * 4. 相同 → 密码正确，不同 → 密码错误
     *
     * 这个过程不需要知道原始密码，只需要比对哈希值。
     *
     * @param request 登录请求体，包含两个必填字段：
     *                - email：登录邮箱
     *                - password：登录密码（明文，HTTPS 保证传输安全）
     * @return AuthResponse 登录成功后的认证响应，包含：
     *         - userId：用户 ID
     *         - email：用户邮箱
     *         - nickname：用户昵称
     *         - role：用户角色
     *         - accessToken：访问令牌（2小时有效）
     *         - refreshToken：刷新令牌（7天有效）
     * @throws BusinessException 当邮箱不存在或密码错误时抛出，错误码 401（Unauthorized）
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: email={}", request.getEmail());

        // ──── 第①步：根据邮箱查询用户记录 ────
        // 使用 LambdaQueryWrapper 构建查询条件
        // 等价于 SQL：SELECT * FROM tb_user WHERE email = ? AND deleted = 0 LIMIT 1
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );

        // 如果查询结果为 null，说明这个邮箱没有注册过
        // 注意：错误信息不写"邮箱不存在"，而是写"邮箱或密码错误"
        // 这是为了防止攻击者探测系统中注册了哪些邮箱
        if (user == null) {
            log.warn("Login failed: user not found, email={}", request.getEmail());
            throw new BusinessException(401, "Invalid email or password");
        }

        // ──── 第②步：验证密码是否正确 ────
        // passwordEncoder.matches(明文, 密文) 的验证过程：
        // 1. 从密文（数据库中存储的 BCrypt 哈希值）中提取盐值
        // 2. 用相同的盐值对用户输入的明文密码进行哈希
        // 3. 比较两次哈希结果是否相同
        // 注意：matches() 方法不是简单的字符串比较！
        //   错误做法：request.getPassword().equals(user.getPassword())  ← 绝对不能这样做！
        //   正确做法：passwordEncoder.matches(明文, 密文)              ← 这才是安全的做法
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password, email={}", request.getEmail());
            // 同样不区分"密码错误"和"邮箱不存在"，统一返回相同的错误信息
            throw new BusinessException(401, "Invalid email or password");
        }

        log.info("Login successful: userId={}, email={}", user.getId(), user.getEmail());

        // ──── 第③步：生成 Access Token 和 Refresh Token ────
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 构建认证响应对象
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 刷新访问令牌 —— 用 Refresh Token 换取新的 Token 对
     *
     * ═══════════════════════════════════════════════════════════════════
     * 这个方法做了什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 刷新令牌流程分为 4 步：
     * ① 验证 Refresh Token 的签名和有效期（是否过期、是否被篡改）
     * ② 校验 Token 类型必须是 "refresh"（防止 Access Token 被用来刷新）
     * ③ 从 Token 中提取用户信息，验证用户是否仍然存在
     * ④ 生成新的 Access Token 和 Refresh Token 返回给前端
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么需要刷新令牌机制？
     * ═══════════════════════════════════════════════════════════════════
     *
     * Access Token 有效期短（2小时），过期后用户需要重新登录。
     * 但频繁登录体验很差，所以设计了 Refresh Token：
     * - Refresh Token 有效期长（7天），可以在 Access Token 过期后换新的
     * - 用户在 7 天内不需要重新输入密码
     * - 如果 Refresh Token 也过期了，才需要重新登录
     *
     * 这样设计的好处：
     * - Access Token 短期有效 → 即使被窃取，危害时间有限
     * - Refresh Token 长期有效 → 用户不需要频繁登录
     * - 两种 Token 分离 → 安全性和便利性的平衡
     *
     * ═══════════════════════════════════════════════════════════════════
     * Token 类型校验的重要性
     * ═══════════════════════════════════════════════════════════════════
     *
     * JWT Token 中包含一个 "type" 字段，标记这是 Access Token 还是 Refresh Token。
     * 刷新令牌时必须校验 type=refresh，原因：
     * - 如果不校验，攻击者拿到 Access Token 后也可以用来刷新
     * - Access Token 可能通过 URL 参数、日志等途径泄露
     * - 校验 type 后，只有 Refresh Token 才能刷新，增加了安全屏障
     *
     * 校验顺序很重要：
     * 1. 先验证签名和有效期（validateToken）→ 确保 Token 本身是合法的
     * 2. 再校验类型（getTokenType）→ 确保是正确类型的 Token
     * 3. 最后验证用户存在（selectById）→ 确保用户没有被删除
     *
     * @param request 刷新令牌请求体，包含一个必填字段：
     *                - refreshToken：刷新令牌字符串
     * @return AuthResponse 刷新成功后的认证响应，包含：
     *         - userId：用户 ID
     *         - email：用户邮箱
     *         - nickname：用户昵称
     *         - role：用户角色
     *         - accessToken：新的访问令牌（2小时有效）
     *         - refreshToken：新的刷新令牌（7天有效）
     * @throws BusinessException 当 Token 无效、类型错误或用户不存在时抛出，错误码 401
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // ──── 第①步：验证 Token 的签名和有效期 ────
        // jwtTokenProvider.validateToken() 会检查：
        // 1. 签名是否正确（Token 没有被篡改）
        // 2. 是否在有效期内（没有过期）
        // 3. 格式是否正确（是合法的 JWT 格式）
        // 如果以上任何一项校验失败，都会返回 false
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid or expired token");
            throw new BusinessException(401, "Invalid or expired refresh token");
        }

        // ──── 第②步：校验 Token 类型必须是 "refresh" ────
        // jwtTokenProvider.getTokenType() 从 Token 的 Claims 中提取 "type" 字段
        // Access Token 的 type="access"，Refresh Token 的 type="refresh"
        // 这里只允许 type="refresh" 的 Token 执行刷新操作
        // 如果有人拿着 Access Token 来刷新，会被拒绝
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token refresh failed: wrong token type={}", tokenType);
            throw new BusinessException(401, "Invalid token type: expected 'refresh' but got '" + tokenType + "'");
        }

        // ──── 第③步：从 Token 中提取用户信息，验证用户是否仍然存在 ────
        // 从 Token 的 subject 字段提取用户 ID
        // subject 字段在生成 Token 时设置为用户 ID 的字符串形式
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        // 从 Token 的自定义声明 "email" 中提取用户邮箱
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 验证用户是否仍然存在于数据库中
        // 可能存在这种情况：用户已注册，Token 还没过期，但用户已被删除（逻辑删除）
        // 这时需要拒绝刷新，防止已删除用户的 Token 继续使用
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("Token refresh failed: user not found, userId={}", userId);
            throw new BusinessException(401, "User not found");
        }

        log.info("Token refresh successful: userId={}", userId);

        // ──── 第④步：生成新的 Token 对 ────
        // 每次刷新都会生成全新的 Access Token 和 Refresh Token
        // 旧的 Token 虽然还在有效期内，但由于是无状态 JWT，无法主动使其失效
        // 安全建议：如果需要更强的安全性，可以实现 Token 黑名单机制
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        // 构建认证响应对象，返回新的 Token 对
        return AuthResponse.builder()
                .userId(userId)
                .email(email)
                .nickname(user.getNickname())
                .role(user.getRole())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
