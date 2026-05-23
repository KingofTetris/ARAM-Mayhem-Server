package com.aram.mayhem.config;

import com.aram.mayhem.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 *
 * 这个类是整个后端的安全"守门员"，决定了哪些请求可以放行、哪些需要登录才能访问。
 * 就像一栋大楼的门禁系统：大堂（公开接口）谁都能进，办公室（需认证接口）需要刷卡。
 *
 * 核心设计思路：
 * 1. 无状态会话（STATELESS）：服务器不保存用户的登录状态，每次请求都通过 JWT Token 来识别身份
 *    - 好处：服务器重启不会让用户掉线，也方便将来做集群部署
 *    - 原理：用户登录后拿到一个"电子通行证"（JWT Token），之后每次请求都带上这个通行证
 *
 * 2. 路径权限分级：
 *    - 公开路径（permitAll）：注册/登录、英雄列表、符文列表、公告、策略的查看 —— 这些不需要登录
 *    - 需认证路径（authenticated）：发布策略、投票、个人中心等 —— 必须登录才能用
 *
 * 3. 密码加密：使用 BCrypt 算法，即使数据库泄露，攻击者也无法还原出原始密码
 *
 * 4. JWT 过滤器：在 Spring Security 原有的过滤器链中，插入我们自定义的 JWT 验证过滤器
 *    - 位置：在 UsernamePasswordAuthenticationFilter 之前执行
 *    - 作用：从请求头中提取 Token，验证有效性，设置认证信息
 *
 * 关联类：
 * - JwtAuthenticationFilter：自定义的 JWT 过滤器，负责解析和验证 Token
 * - JwtTokenProvider：Token 的生成和解析工具
 *
 * @see JwtAuthenticationFilter
 */
@Configuration // 告诉 Spring 这是一个配置类，Spring 启动时会自动加载它
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能
@EnableMethodSecurity // 启用方法级别的安全控制（如 @PreAuthorize 注解），用于 AdminController 等需要特定角色的接口
public class SecurityConfig {

    // 注入自定义的 JWT 过滤器，用于在请求到达 Controller 之前验证 Token
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造函数注入 JWT 过滤器
     * Spring 会自动找到 JwtAuthenticationFilter 的 Bean 实例并注入进来
     *
     * @param jwtAuthenticationFilter 自定义的 JWT 认证过滤器
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置安全过滤器链 —— 这是整个安全配置的核心方法
     *
     * SecurityFilterChain 就像一条安检流水线，每个请求都要经过这道流水线的检查。
     * 我们在这里定义了流水线的每个环节：
     *
     * @param HttpSecurity http Spring 提供的安全配置构建器，用来组装安全规则
     * @return SecurityFilterChain 配置好的安全过滤器链
     * @throws Exception 配置过程中可能出现的异常
     */
    @Bean // 将方法的返回值注册为 Spring Bean，让 Spring Security 使用这个配置
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 第一步：关闭 CSRF 防护
                // CSRF（跨站请求伪造）是一种攻击方式，攻击者诱导用户在已登录的网站上执行非预期操作
                // 我们使用 JWT 无状态认证，不依赖 Cookie，所以 CSRF 攻击对我们无效，可以安全地关闭
                .csrf(AbstractHttpConfigurer::disable)

                // 第二步：配置会话管理策略为"无状态"
                // STATELESS 意味着 Spring Security 不会创建 HttpSession 来保存用户的登录状态
                // 每次请求都是独立的，必须自己携带 Token 来证明身份
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 第三步：配置 URL 路径的访问权限
                .authorizeHttpRequests(auth -> auth
                        // 认证相关接口 —— 公开访问（注册和登录当然不需要登录才能访问）
                        .requestMatchers("/api/auth/**").permitAll()
                        // 英雄数据接口 —— 公开访问（浏览英雄列表和详情不需要登录）
                        .requestMatchers("/api/heroes/**").permitAll()
                        // 符文数据接口 —— 公开访问（浏览符文不需要登录）
                        .requestMatchers("/api/augments/**").permitAll()
                        // 公告接口 —— 公开访问（查看公告不需要登录）
                        .requestMatchers("/api/bulletins/**").permitAll()
                        // 策略的 GET 请求 —— 公开访问（浏览社区策略不需要登录，但发布/投票需要登录）
                        .requestMatchers("GET", "/api/strategies/**").permitAll()
                        // Swagger API 文档 —— 公开访问（方便开发调试）
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // 健康检查端点 —— 公开访问（用于部署监控）
                        .requestMatchers("/actuator/health").permitAll()
                        // 其他所有请求 —— 必须认证（兜底规则：没明确放行的都需要登录）
                        .anyRequest().authenticated()
                )

                // 第四步：在 Spring Security 的过滤器链中插入我们的 JWT 过滤器
                // addFilterBefore 表示在某个过滤器之前执行
                // UsernamePasswordAuthenticationFilter 是 Spring Security 默认的表单登录过滤器
                // 我们在它之前执行 JWT 过滤器，这样如果 Token 有效，就直接设置认证信息，跳过表单登录
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 构建并返回配置好的安全过滤器链
        return http.build();
    }

    /**
     * 密码编码器 —— 使用 BCrypt 算法加密密码
     *
     * BCrypt 是一种专门为密码存储设计的哈希算法，它的特点是：
     * 1. 每次加密同样的密码，生成的密文都不同（因为内置了随机盐值）
     * 2. 可以通过调整强度因子来增加计算成本，抵御暴力破解
     * 3. 即使数据库泄露，攻击者也无法从密文反推出原始密码
     *
     * 使用场景：
     * - 用户注册时：passwordEncoder.encode("原始密码") → 存储加密后的密文
     * - 用户登录时：passwordEncoder.matches("用户输入的密码", "数据库中的密文") → 判断是否匹配
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder 默认强度因子为 10，表示 2^10 = 1024 次哈希迭代
        return new BCryptPasswordEncoder();
    }
}
