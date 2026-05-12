package com.aram.mayhem.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 * 测试覆盖：Token 生成、type claim、验证、异常处理
 */
@DisplayName("JwtTokenProvider 测试")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Base64 编码的 256 位密钥（jjwt 要求 HMAC-SHA256 密钥至少 256 位）
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-purposes-only!!".getBytes()
    );
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;   // 15 分钟
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7 天

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );
    }

    // ============================================================
    // Token 生成与 type claim 测试
    // ============================================================

    @Test
    @DisplayName("testGenerateAccessToken_containsTypeClaim - Access Token 应包含 type='access' claim")
    void testGenerateAccessToken_containsTypeClaim() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email);

        // Then
        assertNotNull(accessToken);
        assertFalse(accessToken.isEmpty());

        // 解析 token 验证 type claim
        String type = jwtTokenProvider.getTokenType(accessToken);
        assertEquals("access", type);

        // 验证 userId 和 email 也正确
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(accessToken));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(accessToken));
    }

    @Test
    @DisplayName("testGenerateRefreshToken_containsTypeClaim - Refresh Token 应包含 type='refresh' claim")
    void testGenerateRefreshToken_containsTypeClaim() {
        // Given
        Long userId = 2L;
        String email = "user@example.com";

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());

        // 解析 token 验证 type claim
        String type = jwtTokenProvider.getTokenType(refreshToken);
        assertEquals("refresh", type);

        // 验证 userId 和 email 也正确
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(refreshToken));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(refreshToken));
    }

    // ============================================================
    // getTokenType 方法测试
    // ============================================================

    @Test
    @DisplayName("testGetTokenType_returnsCorrectType - getTokenType 应正确返回 type claim")
    void testGetTokenType_returnsCorrectType() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com");
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L, "test@example.com");

        // When & Then
        assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
        assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
    }

    // ============================================================
    // validateToken 有效 Token 测试
    // ============================================================

    @Test
    @DisplayName("testValidateToken_validToken_returnsTrue - 有效 Token 应通过验证")
    void testValidateToken_validToken_returnsTrue() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // When & Then
        assertTrue(jwtTokenProvider.validateToken(validToken));
    }

    @Test
    @DisplayName("testValidateToken_validRefreshToken_returnsTrue - 有效 Refresh Token 应通过验证")
    void testValidateToken_validRefreshToken_returnsTrue() {
        // Given
        String validToken = jwtTokenProvider.generateRefreshToken(1L, "test@example.com");

        // When & Then
        assertTrue(jwtTokenProvider.validateToken(validToken));
    }

    // ============================================================
    // validateToken 过期 Token 测试
    // ============================================================

    @Test
    @DisplayName("testValidateToken_expiredToken_returnsFalse - 过期 Token 应被拒绝")
    void testValidateToken_expiredToken_returnsFalse() {
        // Given：手动构造一个已过期的 Token
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "access")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)) // 2 小时前
                .expiration(new java.util.Date(System.currentTimeMillis() - 60 * 60 * 1000))      // 1 小时前过期
                .signWith(secretKey)
                .compact();

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    // ============================================================
    // validateToken 篡改 Token 测试
    // ============================================================

    @Test
    @DisplayName("testValidateToken_tamperedToken_returnsFalse - 篡改签名后的 Token 应被拒绝")
    void testValidateToken_tamperedToken_returnsFalse() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // 篡改 Token 的最后一位字符（破坏签名）
        String tamperedToken = validToken.substring(0, validToken.length() - 1) +
                (validToken.charAt(validToken.length() - 1) == 'a' ? 'b' : 'a');

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("testValidateToken_tamperedPayload_returnsFalse - 篡改 Payload 的 Token 应被拒绝")
    void testValidateToken_tamperedPayload_returnsFalse() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // 篡改 payload 部分（base64url 中间部分）
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length);

        // 修改中间部分，但保持签名不变
        String tamperedPayload = parts[1] + "x";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    // ============================================================
    // validateToken 异常输入测试
    // ============================================================

    @Test
    @DisplayName("testValidateToken_nullToken_returnsFalse - null Token 应被拒绝")
    void testValidateToken_nullToken_returnsFalse() {
        // When & Then
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("testValidateToken_emptyToken_returnsFalse - 空字符串 Token 应被拒绝")
    void testValidateToken_emptyToken_returnsFalse() {
        // When & Then
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("testValidateToken_randomString_returnsFalse - 随机字符串 Token 应被拒绝")
    void testValidateToken_randomString_returnsFalse() {
        // Given
        String randomString = "this-is-not-a-valid-jwt-token";

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(randomString));
    }

    @Test
    @DisplayName("testValidateToken_malformedJwt_returnsFalse - 格式错误的 JWT 应被拒绝")
    void testValidateToken_malformedJwt_returnsFalse() {
        // Given：缺少 signature 部分
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0";

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    @DisplayName("testValidateToken_wrongSecret_returnsFalse - 使用不同密钥签发的 Token 应被拒绝")
    void testValidateToken_wrongSecret_returnsFalse() {
        // Given：使用另一个密钥签发 Token
        String otherSecret = Base64.getEncoder().encodeToString(
                "another-very-long-secret-key-for-testing-purposes-only!!".getBytes()
        );
        SecretKey otherKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(otherSecret));
        String tokenWithWrongSecret = Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "access")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(otherKey)
                .compact();

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(tokenWithWrongSecret));
    }

    // ============================================================
    // getUserIdFromToken / getEmailFromToken 测试
    // ============================================================

    @Test
    @DisplayName("testGetUserIdFromToken_correctUserId - 应正确提取 userId")
    void testGetUserIdFromToken_correctUserId() {
        // Given
        Long userId = 42L;
        String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com");

        // When
        Long extractedId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedId);
    }

    @Test
    @DisplayName("testGetEmailFromToken_correctEmail - 应正确提取 email")
    void testGetEmailFromToken_correctEmail() {
        // Given
        String email = "user@domain.com";
        String token = jwtTokenProvider.generateAccessToken(1L, email);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("testGetUserIdFromToken_expiredToken_throwsException - 过期 Token 提取 userId 应抛异常")
    void testGetUserIdFromToken_expiredToken_throwsException() {
        // Given：构造过期 Token
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("type", "access")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000))
                .expiration(new java.util.Date(System.currentTimeMillis() - 60 * 60 * 1000))
                .signWith(secretKey)
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> jwtTokenProvider.getUserIdFromToken(expiredToken));
    }
}
