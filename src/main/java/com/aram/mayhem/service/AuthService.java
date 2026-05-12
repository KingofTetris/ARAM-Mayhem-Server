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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (existing != null) {
            throw new BusinessException(409, "Email already registered");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setDisplayMode(0);
        user.setNotificationEnabled(1);
        user.setAvatarUrl("");

        userMapper.insert(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (user == null) {
            throw new BusinessException(401, "Invalid email or password");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(401, "Invalid or expired refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(401, "Invalid token type: expected 'refresh' but got '" + tokenType + "'");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "User not found");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

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
