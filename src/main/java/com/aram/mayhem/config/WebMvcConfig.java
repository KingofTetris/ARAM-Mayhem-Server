package com.aram.mayhem.config;

import com.aram.mayhem.common.logging.RequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * Spring MVC 是 Spring 框架中处理 Web 请求的核心模块。
 * 这个配置类用于自定义 Spring MVC 的行为，目前主要功能是注册请求日志拦截器。
 *
 * 拦截器（Interceptor）就像高速公路上的收费站：
 * - 每个请求（车辆）到达 Controller（目的地）之前，都要先经过拦截器（收费站）
 * - 拦截器可以记录请求信息、检查权限、修改请求等
 * - 我们用拦截器来记录每个 API 请求的日志，方便排查问题
 *
 * 本类配置的拦截器：
 * - RequestLoggingInterceptor：记录请求的方法、路径、耗时等信息
 * - 拦截路径：/api/**（所有 API 请求）
 * - 排除路径：/api/auth/**（登录注册请求，避免大量登录日志）
 *
 * 关联类：
 * - RequestLoggingInterceptor：具体的日志记录逻辑
 *
 * @see RequestLoggingInterceptor
 */
@Configuration // 声明这是一个 Spring MVC 配置类
public class WebMvcConfig implements WebMvcConfigurer { // 实现 WebMvcConfigurer 接口，可以自定义 MVC 配置

    // 注入请求日志拦截器
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    /**
     * 构造函数注入拦截器
     * Spring 会自动找到 RequestLoggingInterceptor 的 Bean 并注入
     *
     * @param requestLoggingInterceptor 请求日志拦截器实例
     */
    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    /**
     * 注册拦截器 —— 告诉 Spring MVC 哪些请求需要经过这个拦截器
     *
     * 这个方法在 Spring MVC 初始化时自动调用，我们在这里配置拦截器的拦截规则：
     * - addPathPatterns：要拦截的路径（白名单模式，只有匹配的路径才会被拦截）
     * - excludePathPatterns：要排除的路径（即使匹配了拦截路径，这些路径也不会被拦截）
     *
     * @param registry 拦截器注册表，Spring MVC 提供的，用于注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor) // 注册我们的日志拦截器
                .addPathPatterns("/api/**") // 拦截所有 /api/ 开头的请求
                .excludePathPatterns("/api/auth/**"); // 排除 /api/auth/ 开头的请求（登录注册）
    }
}
