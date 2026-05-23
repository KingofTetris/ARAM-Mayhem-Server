package com.aram.mayhem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS 跨域配置类
 *
 * CORS（Cross-Origin Resource Sharing，跨域资源共享）是浏览器的一种安全机制。
 * 当前端（运行在 localhost:3000）和后端（运行在 localhost:8080）的端口不同时，
 * 浏览器会阻止前端直接访问后端的接口，这就是"跨域问题"。
 *
 * 就像两个不同小区之间有围墙，A 小区的居民不能随意进入 B 小区。
 * CORS 配置就是在围墙上开一个门，允许特定的人（或所有人）通过。
 *
 * 本配置的作用：
 * - 允许所有来源（Origin）的前端访问后端 API
 * - 允许携带凭证（Cookie、Authorization 头等）
 * - 允许所有 HTTP 方法和请求头
 * - 预检请求缓存 1 小时，减少浏览器的 OPTIONS 请求次数
 *
 * 安全注意事项：
 * - 当前使用 allowedOriginPatterns("*") 允许所有来源，仅适用于开发环境
 * - 生产环境必须替换为具体的前端域名列表，例如：
 *   allowedOriginPatterns(List.of("https://aram-mayhem.com", "https://admin.aram-mayhem.com"))
 * - 不能同时使用 allowedOrigins("*") 和 allowCredentials(true)，这是 CORS 规范禁止的
 * - allowedOriginPatterns("*") 是 Spring 6.0+ 推荐的方式，可以和 allowCredentials 共存
 */
@Configuration // 声明这是一个 Spring 配置类
public class CorsConfig {

    /**
     * 创建 CORS 过滤器 Bean
     *
     * CorsFilter 是一个 Servlet 过滤器，会在每个 HTTP 请求到达 Controller 之前，
     * 给响应头添加 CORS 相关的字段（如 Access-Control-Allow-Origin），
     * 告诉浏览器"这个请求是被允许的"。
     *
     * 对于浏览器的预检请求（OPTIONS 方法），CorsFilter 也会直接返回允许的响应，
     * 不会让请求到达 Controller。
     *
     * @return CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源（Origin）—— 哪些前端地址可以访问后端
        // "*" 表示允许所有来源，生产环境应改为具体域名
        config.setAllowedOriginPatterns(List.of("*"));

        // 允许的 HTTP 方法 —— 前端可以使用哪些 HTTP 方法访问后端
        // GET（查询）、POST（创建）、PUT（全量更新）、PATCH（部分更新）、DELETE（删除）、OPTIONS（预检）
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 允许的请求头 —— 前端可以在请求中携带哪些自定义头
        // "*" 表示允许所有请求头，包括 Authorization（JWT Token）、Content-Type 等
        config.setAllowedHeaders(List.of("*"));

        // 是否允许携带凭证 —— 前端请求是否可以携带 Cookie 和 Authorization 头
        // true 表示允许，这对于 JWT Token 认证是必需的
        config.setAllowCredentials(true);

        // 预检请求的缓存时间（单位：秒）
        // 浏览器在发送跨域请求前，会先发一个 OPTIONS 请求（预检）询问服务器是否允许
        // 缓存 3600 秒 = 1 小时，意味着同一来源的预检结果在 1 小时内不会重复发送
        config.setMaxAge(3600L);

        // 创建 CORS 配置源，将上面的配置应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // "/**" 表示所有 URL 路径都应用这个 CORS 配置
        source.registerCorsConfiguration("/**", config);

        // 创建并返回 CORS 过滤器
        return new CorsFilter(source);
    }
}
