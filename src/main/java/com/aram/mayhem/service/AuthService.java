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
 * 认证服务
 *
 * 功能：用户注册、用户登录、令牌刷新
 * 关联：UserMapper, JwtTokenProvider, PasswordEncoder
 * 安全：密码使用 BCrypt 加密，JWT Token 区分 access/refresh 类型
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 构造函数注入依赖
     *
     * @param userMapper         用户数据访问层
     * @param passwordEncoder    密码加密器（BCrypt）
     * @param jwtTokenProvider   JWT令牌生成器
     */
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户注册（认证模块）
     *
     * 流程：验证邮箱唯一性 → 创建用户记录 → 密码BCrypt加密 → 生成Token
     * 密码规则：至少8位，包含字母和数字（由DTO校验）
     *
     * @param request 注册请求体（邮箱、密码、昵称）
     * @return AuthResponse 注册成功返回用户信息和Token
     * @throws BusinessException 邮箱已注册时抛出409错误
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("User registration attempt: email={}", request.getEmail());

        // 检查邮箱是否已注册
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (existing != null) {
            log.warn("Registration failed: email already registered, email={}", request.getEmail());
            throw new BusinessException(409, "Email already registered");
        }

        // 创建用户实体并设置默认值
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt加密
        user.setNickname(request.getNickname());
        user.setRole("USER");           // 默认普通用户角色
        user.setDisplayMode(0);         // 默认显示模式
        user.setNotificationEnabled(1); // 默认开启通知
        user.setAvatarUrl("");          // 默认空头像

        // 插入用户记录
        userMapper.insert(user);
        log.info("User registered successfully: userId={}, email={}", user.getId(), user.getEmail());

        // 生成Access Token和Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 构建响应
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
     * 用户登录（认证模块）
     *
     * 流程：查询用户 → 验证密码 → 生成Token
     * 安全：密码验证使用BCrypt匹配，不存储明文密码
     *
     * @param request 登录请求体（邮箱、密码）
     * @return AuthResponse 登录成功返回用户信息和Token
     * @throws BusinessException 邮箱或密码错误时抛出401错误
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: email={}", request.getEmail());

        // 根据邮箱查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (user == null) {
            log.warn("Login failed: user not found, email={}", request.getEmail());
            throw new BusinessException(401, "Invalid email or password");
        }

        // 验证密码（BCrypt匹配）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password, email={}", request.getEmail());
            throw new BusinessException(401, "Invalid email or password");
        }

        log.info("Login successful: userId={}, email={}", user.getId(), user.getEmail());

        // 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 构建响应
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
     * 刷新访问令牌（认证模块）
     *
     * 流程：验证Refresh Token有效性 → 校验Token类型 → 查询用户 → 生成新Token对
     * 安全：刷新后旧Refresh Token失效，防止Token泄漏被滥用
     *
     * @param request 刷新请求体（Refresh Token）
     * @return AuthResponse 刷新成功返回新的Token对
     * @throws BusinessException Token无效或用户不存在时抛出401错误
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 验证Token签名和有效期
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid or expired token");
            throw new BusinessException(401, "Invalid or expired refresh token");
        }

        // 校验Token类型必须为refresh
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token refresh failed: wrong token type={}", tokenType);
            throw new BusinessException(401, "Invalid token type: expected 'refresh' but got '" + tokenType + "'");
        }

        // 从Token中提取用户信息
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("Token refresh failed: user not found, userId={}", userId);
            throw new BusinessException(401, "User not found");
        }

        log.info("Token refresh successful: userId={}", userId);

        // 生成新的Token对（旧Token自动失效）
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        // 构建响应
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
