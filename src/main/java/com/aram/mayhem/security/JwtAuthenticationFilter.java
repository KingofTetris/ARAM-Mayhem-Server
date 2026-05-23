package com.aram.mayhem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * 这个过滤器是 JWT 认证流程的"前台接待员"。
 * 每个到达后端的 HTTP 请求，都会先经过这个过滤器检查：
 * "你带身份证（JWT Token）了吗？身份证是真的吗？"
 *
 * 工作流程（每个请求都会执行）：
 * 1. 从请求头中提取 Token（Authorization: Bearer xxx）
 * 2. 验证 Token 是否有效（签名正确、未过期）
 * 3. 从 Token 中提取用户邮箱
 * 4. 根据邮箱从数据库加载用户信息
 * 5. 将用户信息设置到 Spring Security 的安全上下文中
 * 6. 继续执行后续过滤器和 Controller
 *
 * 如果 Token 无效或用户不存在：
 * - 不会抛出异常，而是清除安全上下文，让请求继续走
 * - 后续的权限检查（如 @PreAuthorize）会发现用户未认证，返回 401 或 403
 *
 * 继承 OncePerRequestFilter 的原因：
 * - 确保每个请求只经过一次这个过滤器
 * - 避免在请求转发（forward）时重复执行
 *
 * 执行时机：
 * - 在 UsernamePasswordAuthenticationFilter 之前（由 SecurityConfig 配置）
 * - 在 CorsFilter 之后（CORS 预检请求先处理）
 *
 * 关联类：
 * - JwtTokenProvider：Token 的解析和验证工具
 * - CustomUserDetailsService：根据邮箱加载用户信息
 * - SecurityConfig：配置过滤器的注册和执行顺序
 *
 * @see JwtTokenProvider
 * @see CustomUserDetailsService
 * @see com.aram.mayhem.config.SecurityConfig
 */
@Component // 注册为 Spring 组件，SecurityConfig 中通过构造函数注入
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT 工具类，用于解析和验证 Token
    private final JwtTokenProvider jwtTokenProvider;
    // 用户详情服务，用于根据邮箱从数据库加载用户信息
    private final UserDetailsService userDetailsService;

    /**
     * 构造函数注入依赖
     *
     * @param jwtTokenProvider JWT 工具类
     * @param userDetailsService 用户详情服务（Spring 会注入 CustomUserDetailsService 实例）
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 过滤器的核心方法 —— 每个请求都会执行这个方法
     *
     * 这是 Spring Security 过滤器链中的一个环节。
     * 方法名 doFilterInternal 表示"在过滤器链内部执行"。
     *
     * 执行流程：
     * 1. 提取 Token → 2. 验证 Token → 3. 加载用户 → 4. 设置认证 → 5. 继续过滤链
     *
     * @param request HTTP 请求对象，包含请求头、参数等信息
     * @param response HTTP 响应对象，用于返回响应数据
     * @param filterChain 过滤器链，调用 doFilter 继续执行后续过滤器和 Controller
     * @throws ServletException Servlet 相关异常
     * @throws IOException IO 相关异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 第一步：从请求头中提取 JWT Token
        String token = extractToken(request);

        // 第二步：检查 Token 是否存在且有效
        // StringUtils.hasText 检查字符串不为 null、不为空、不全为空白字符
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 第三步：从 Token 中提取用户邮箱
            String email = jwtTokenProvider.getEmailFromToken(token);
            try {
                // 第四步：根据邮箱从数据库加载用户信息
                // userDetailsService.loadUserByUsername 会查询数据库，返回 UserDetails 对象
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 第五步：创建 Spring Security 的认证对象
                // UsernamePasswordAuthenticationToken 是 Spring Security 的标准认证令牌
                // 三个参数分别是：用户主体（UserDetails）、凭证（null，因为已认证）、权限列表
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // 第六步：将认证信息设置到安全上下文中
                // SecurityContextHolder 是 Spring Security 的"安全上下文持有者"
                // 设置后，后续的 Controller 和 Service 可以通过 SecurityContextHolder 获取当前登录用户
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 如果用户不存在（邮箱在数据库中找不到），清除安全上下文
                // 这样后续的权限检查会发现用户未认证，返回 401 未授权
                SecurityContextHolder.clearContext();
            }
        }

        // 第七步：继续执行过滤器链（无论认证成功与否，都要继续执行）
        // 这样即使没有 Token 的请求也能到达 Controller，由权限注解决定是否放行
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT Token —— 内部辅助方法
     *
     * JWT Token 通常放在 HTTP 请求的 Authorization 头中，格式为：
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *
     * "Bearer" 是一种认证方案，表示"持有此 Token 的人就是合法用户"。
     * 我们需要去掉 "Bearer " 前缀，只保留 Token 本身。
     *
     * @param request HTTP 请求对象
     * @return Token 字符串，如果不存在则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // 获取 Authorization 请求头的值
        String bearerToken = request.getHeader("Authorization");
        // 检查值是否存在且以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 截取 "Bearer " 后面的部分（跳过前 7 个字符 "Bearer "）
            return bearerToken.substring(7);
        }
        // 没有 Token，返回 null
        return null;
    }
}
