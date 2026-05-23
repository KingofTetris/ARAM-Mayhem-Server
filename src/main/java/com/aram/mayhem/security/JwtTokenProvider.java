package com.aram.mayhem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 令牌生成与校验工具类
 *
 * JWT（JSON Web Token）是一种安全的令牌格式，用于在客户端和服务器之间传递身份信息。
 * 就像一张"电子身份证"，上面盖了公章（签名），别人无法伪造。
 *
 * JWT 的结构由三部分组成，用 "." 分隔：
 * 1. Header（头部）：声明令牌类型和签名算法
 * 2. Payload（载荷）：存放实际数据（如用户 ID、邮箱、令牌类型）
 * 3. Signature（签名）：用密钥对前两部分签名，防止数据被篡改
 *
 * 本项目使用两种 Token：
 * - Access Token（访问令牌）：
 *   - 短期有效（默认 2 小时），用于日常 API 访问
 *   - 包含 type=access 标记，防止 Refresh Token 被当作 Access Token 使用
 *   - 就像酒店的房卡，有效期只有住店期间
 *
 * - Refresh Token（刷新令牌）：
 *   - 长期有效（默认 7 天），用于刷新 Access Token
 *   - 包含 type=refresh 标记
 *   - 就像酒店的预订确认信，可以用来续住（获取新的房卡）
 *
 * 安全设计：
 * - 使用 HMAC-SHA 算法签名，密钥从配置文件读取（Base64 编码）
 * - Token 中不存储敏感信息（如密码），只存 userId、email、type
 * - Token 过期后自动失效，必须重新登录或用 Refresh Token 刷新
 *
 * 配置项（application.yml）：
 * - jwt.secret：签名密钥（Base64 编码的字符串）
 * - jwt.access-token-expiration：Access Token 过期时间（毫秒）
 * - jwt.refresh-token-expiration：Refresh Token 过期时间（毫秒）
 *
 * 关联类：
 * - JwtAuthenticationFilter：在请求中提取和验证 Token
 * - AuthService：调用本类生成和解析 Token
 *
 * @see JwtAuthenticationFilter
 * @see com.aram.mayhem.service.AuthService
 */
@Component // 将这个类注册为 Spring 组件，其他类可以通过 @Autowired 注入使用
public class JwtTokenProvider {

    // 签名密钥 —— 用于给 Token 加签名和验证签名，就像公章一样
    private final SecretKey secretKey;
    // Access Token 过期时间（毫秒）
    private final long accessTokenExpiration;
    // Refresh Token 过期时间（毫秒）
    private final long refreshTokenExpiration;

    /**
     * 构造函数 —— 初始化签名密钥和过期时间
     *
     * @param secret 从配置文件读取的 Base64 编码密钥字符串
     * @param accessTokenExpiration Access Token 过期时间（毫秒）
     * @param refreshTokenExpiration Refresh Token 过期时间（毫秒）
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // 将 Base64 编码的密钥字符串解码为字节数组
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        // 使用解码后的字节数组创建 HMAC-SHA 密钥对象
        // Keys.hmacShaKeyFor 会根据密钥长度自动选择 SHA-256/384/512 算法
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成 Access Token（访问令牌）
     *
     * Access Token 是用户登录后获得的"短期通行证"，用于访问需要认证的 API。
     * 每次请求时在 Authorization 头中携带此 Token，服务器验证后允许访问。
     *
     * Token 中包含的信息（Claims）：
     * - subject：用户 ID（作为 Token 的主体标识）
     * - email：用户邮箱
     * - type："access"（标记这是 Access Token，防止和 Refresh Token 混用）
     * - issuedAt：签发时间
     * - expiration：过期时间
     *
     * @param userId 用户 ID
     * @param email 用户邮箱
     * @return 生成的 JWT Access Token 字符串
     */
    public String generateAccessToken(Long userId, String email) {
        // 当前时间 —— 作为 Token 的签发时间
        Date now = new Date();
        // 过期时间 = 当前时间 + Access Token 有效期
        Date expiry = new Date(now.getTime() + accessTokenExpiration);
        // 构建 JWT Token
        return Jwts.builder()
                .subject(userId.toString()) // 设置主体（用户 ID）
                .claim("email", email) // 自定义声明：用户邮箱
                .claim("type", "access") // 自定义声明：令牌类型为 access
                .issuedAt(now) // 签发时间
                .expiration(expiry) // 过期时间
                .signWith(secretKey) // 使用密钥签名（确保 Token 不被篡改）
                .compact(); // 压缩为字符串格式（xxx.yyy.zzz）
    }

    /**
     * 生成 Refresh Token（刷新令牌）
     *
     * Refresh Token 是"长期通行证"，用于在 Access Token 过期后获取新的 Access Token，
     * 而不需要用户重新输入密码登录。
     *
     * 与 Access Token 的区别：
     * - 有效期更长（7 天 vs 2 小时）
     * - type 标记为 "refresh"，服务器会校验这个标记防止混用
     * - 只用于刷新操作，不用于日常 API 访问
     *
     * @param userId 用户 ID
     * @param email 用户邮箱
     * @return 生成的 JWT Refresh Token 字符串
     */
    public String generateRefreshToken(Long userId, String email) {
        Date now = new Date();
        // Refresh Token 的过期时间比 Access Token 长得多
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);
        return Jwts.builder()
                .subject(userId.toString()) // 主体：用户 ID
                .claim("email", email) // 自定义声明：用户邮箱
                .claim("type", "refresh") // 自定义声明：令牌类型为 refresh
                .issuedAt(now) // 签发时间
                .expiration(expiry) // 过期时间
                .signWith(secretKey) // 使用同一个密钥签名
                .compact();
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * 用户 ID 存储在 JWT 的 subject（主体）字段中。
     * 解析后需要将字符串转换为 Long 类型。
     *
     * @param token JWT Token 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        // 解析 Token 获取所有声明（Claims）
        Claims claims = parseClaims(token);
        // subject 字段存储的是用户 ID 的字符串形式，需要转换为 Long
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中提取用户邮箱
     *
     * 邮箱存储在 JWT 的自定义声明 "email" 中。
     *
     * @param token JWT Token 字符串
     * @return 用户邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        // 从自定义声明中获取 email 字段的值
        return claims.get("email", String.class);
    }

    /**
     * 从 Token 中提取令牌类型
     *
     * 令牌类型存储在 JWT 的自定义声明 "type" 中，值为 "access" 或 "refresh"。
     * 这个方法用于校验 Token 类型，防止 Refresh Token 被当作 Access Token 使用。
     *
     * 安全意义：
     * - 攻击者如果拿到了 Refresh Token，不能直接用它访问 API
     * - 服务器会检查 type 字段，只有 type=access 的 Token 才能通过认证
     *
     * @param token JWT Token 字符串
     * @return 令牌类型（"access" 或 "refresh"）
     */
    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * 校验 Token 是否有效
     *
     * 校验内容包括：
     * 1. 签名是否正确（Token 没有被篡改）
     * 2. 是否在有效期内（没有过期）
     * 3. 格式是否正确（是合法的 JWT 格式）
     *
     * 如果以上任何一项校验失败，都会返回 false。
     *
     * @param token JWT Token 字符串
     * @return true 表示 Token 有效，false 表示无效
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析 Token，如果解析成功说明 Token 有效
            parseClaims(token);
            return true;
        } catch (Exception e) {
            // 任何解析异常都说明 Token 无效（签名错误、过期、格式错误等）
            return false;
        }
    }

    /**
     * 解析 Token 获取所有声明（Claims）—— 内部核心方法
     *
     * Claims 是 JWT 中存储的所有自定义数据的容器。
     * 解析过程会自动验证签名和过期时间，如果验证失败会抛出异常。
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含 Token 中的所有声明数据
     * @throws io.jsonwebtoken.JwtException Token 无效时抛出异常
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 设置验证签名用的密钥
                .build()
                .parseSignedClaims(token) // 解析并验证 Token
                .getPayload(); // 获取 Token 的载荷部分（即 Claims）
    }
}
