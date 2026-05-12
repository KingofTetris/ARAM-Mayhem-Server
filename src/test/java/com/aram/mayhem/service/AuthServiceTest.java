package com.aram.mayhem.service;

import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.AuthResponse;
import com.aram.mayhem.dto.RefreshTokenRequest;
import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.UserMapper;
import com.aram.mayhem.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试
 * 测试覆盖：refreshToken 方法的成功路径、type 校验、无效 Token、用户不存在
 */
@DisplayName("AuthService 测试")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    // 使用真实的 JwtTokenProvider 实例，避免 Mock JWT 行为
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-purposes-only!!".getBytes()
    );
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );
        // 通过构造函数注入（AuthService 使用构造器注入）
        authService = new AuthService(userMapper, passwordEncoder, jwtTokenProvider);
    }

    // ============================================================
    // Refresh Token 成功刷新测试
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withRefreshToken_success - 使用正确的 Refresh Token 刷新成功")
    void testRefreshToken_withRefreshToken_success() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String nickname = "TestUser";
        String role = "USER";

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail(email);
        existingUser.setNickname(nickname);
        existingUser.setRole(role);

        when(userMapper.selectById(userId)).thenReturn(existingUser);

        // When
        AuthResponse response = authService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(email, response.getEmail());
        assertEquals(nickname, response.getNickname());
        assertEquals(role, response.getRole());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        // 验证新 Token 的 type claim 正确
        assertEquals("access", jwtTokenProvider.getTokenType(response.getAccessToken()));
        assertEquals("refresh", jwtTokenProvider.getTokenType(response.getRefreshToken()));
    }

    // ============================================================
    // Access Token 被拒绝测试（BE-014 核心安全测试）
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withAccessToken_throwsException - 使用 Access Token 刷新应抛出 BusinessException")
    void testRefreshToken_withAccessToken_throwsException() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // 故意使用 Access Token 而非 Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(accessToken);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));

        // 验证异常信息包含 type 校验失败提示
        assertEquals(401, exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid token type"));
    }

    // ============================================================
    // 无效 Token 测试
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withInvalidToken_throwsException - 使用无效/篡改 Token 刷新应抛出异常")
    void testRefreshToken_withInvalidToken_throwsException() {
        // Given
        String invalidToken = "this.is.not.a.valid.jwt.token";

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(invalidToken);

        // When & Then
        assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("testRefreshToken_withTamperedToken_throwsException - 使用篡改 Token 刷新应抛出异常")
    void testRefreshToken_withTamperedToken_throwsException() {
        // Given
        String validToken = jwtTokenProvider.generateRefreshToken(1L, "test@example.com");
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "X";

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(tamperedToken);

        // When & Then
        assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
    }

    // ============================================================
    // 用户不存在测试
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withNonExistentUser_throwsException - Token 中用户不存在时应抛出异常")
    void testRefreshToken_withNonExistentUser_throwsException() {
        // Given
        Long userId = 999L;
        String email = "deleted@example.com";

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        // 模拟用户已被删除或不存在
        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));

        assertEquals(401, exception.getCode());
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ============================================================
    // 过期 Token 测试
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withExpiredToken_throwsException - 使用过期 Token 刷新应抛出异常")
    void testRefreshToken_withExpiredToken_throwsException() {
        // Given：手动构造过期 Token
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "refresh")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L)) // 8 天前
                .expiration(new java.util.Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L))  // 1 天前过期
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(expiredToken);

        // When & Then
        assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
    }

    // ============================================================
    // 错误 type 值测试
    // ============================================================

    @Test
    @DisplayName("testRefreshToken_withWrongType_throwsException - type 为任意非 refresh 值时应拒绝")
    void testRefreshToken_withWrongType_throwsException() {
        // Given：手动构造 type 为 "access" 的 Token（等同于直接用 Access Token）
        String tokenWithWrongType = io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "access")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(tokenWithWrongType);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));

        assertEquals(401, exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid token type"));
    }

    @Test
    @DisplayName("testRefreshToken_withUnknownType_throwsException - type 为未知值时应拒绝")
    void testRefreshToken_withUnknownType_throwsException() {
        // Given：手动构造 type 为 "unknown" 的 Token
        String tokenWithUnknownType = io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "unknown")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(tokenWithUnknownType);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));

        assertEquals(401, exception.getCode());
    }
}
