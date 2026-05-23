package com.aram.mayhem.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器
 *
 * 这个拦截器就像一个"门卫记录员"，记录每个进出后端的 HTTP 请求。
 * 当请求进来时记录"谁来了、从哪来"，当请求完成时记录"处理结果如何、花了多久"。
 *
 * 日志输出格式：
 * - 请求开始：>>> GET /api/heroes?page=1 | client=192.168.1.100
 * - 请求结束：<<< GET /api/heroes | status=200 | time=45ms
 *
 * 日志级别策略（根据响应状态码分级）：
 * - 2xx（成功）：INFO 级别 —— 正常请求，不需要特别关注
 * - 4xx（客户端错误）：WARN 级别 —— 可能是恶意请求或前端 Bug
 * - 5xx（服务器错误）：ERROR 级别 —— 服务器出了问题，需要紧急排查
 *
 * 客户端 IP 获取逻辑：
 * 1. 优先从 X-Forwarded-For 头获取（经过 Nginx 等反向代理时的真实 IP）
 * 2. 其次从 X-Real-IP 头获取（Nginx 设置的真实 IP）
 * 3. 最后使用 request.getRemoteAddr()（直接连接的 IP）
 * 4. 如果 X-Forwarded-For 包含多个 IP（经过多层代理），取第一个
 *
 * 注册位置：WebMvcConfig 中注册，拦截 /api/**，排除 /api/auth/**
 *
 * 关联类：
 * - WebMvcConfig：注册此拦截器
 *
 * @see com.aram.mayhem.config.WebMvcConfig
 */
@Component // 注册为 Spring 组件，WebMvcConfig 中通过构造函数注入
public class RequestLoggingInterceptor implements HandlerInterceptor {

    // SLF4J 日志记录器
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    // 请求开始时间的属性名，存储在 request 的 attribute 中
    private static final String START_TIME_ATTR = "requestStartTime";

    /**
     * 请求处理前执行 —— 记录请求开始信息
     *
     * 这个方法在 Controller 方法执行之前被调用。
     * 我们在这里记录请求的方法、URI、查询参数和客户端 IP，
     * 同时把当前时间存入 request 属性，用于后续计算请求耗时。
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler 即将执行的 Controller 方法
     * @return true 表示继续执行后续拦截器和 Controller；false 表示中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 将当前时间戳存入 request 属性，供 afterCompletion 计算耗时
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // 获取请求的 HTTP 方法（GET、POST、PUT、DELETE 等）
        String method = request.getMethod();
        // 获取请求的 URI 路径（如 /api/heroes）
        String uri = request.getRequestURI();
        // 获取查询参数字符串（如 page=1&size=20），可能为 null
        String query = request.getQueryString();
        // 获取客户端的真实 IP 地址
        String clientIp = getClientIp(request);

        // 记录请求开始日志
        // 格式：>>> GET /api/heroes?page=1 | client=192.168.1.100
        log.info(">>> {} {}{} | client={}", method, uri,
                query != null ? "?" + query : "", clientIp);
        // 返回 true，让请求继续执行
        return true;
    }

    /**
     * 请求处理完成后执行 —— 记录请求结果和耗时
     *
     * 这个方法在 Controller 方法执行完毕、视图渲染完成后被调用（无论是否抛出异常）。
     * 我们在这里计算请求的总耗时，并根据响应状态码选择合适的日志级别。
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler 执行的 Controller 方法
     * @param ex Controller 执行过程中抛出的异常（如果没有异常则为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 从 request 属性中取出请求开始时间
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        // 计算请求耗时（毫秒），如果开始时间不存在则返回 -1
        long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        // 获取 HTTP 响应状态码（如 200、404、500）
        int status = response.getStatus();

        // 根据是否有异常和状态码选择日志级别
        if (ex != null) {
            // 有异常：ERROR 级别，记录异常信息
            log.error("<<< {} {} | status={} | time={}ms | error={}", method, uri, status, duration, ex.getMessage());
        } else if (status >= 500) {
            // 5xx 状态码：ERROR 级别（服务器内部错误）
            log.error("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        } else if (status >= 400) {
            // 4xx 状态码：WARN 级别（客户端请求有问题）
            log.warn("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        } else {
            // 2xx/3xx 状态码：INFO 级别（正常请求）
            log.info("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        }
    }

    /**
     * 获取客户端真实 IP 地址 —— 内部辅助方法
     *
     * 当后端部署在 Nginx 等反向代理后面时，request.getRemoteAddr() 获取的是代理服务器的 IP，
     * 而不是客户端的真实 IP。我们需要从代理设置的特殊请求头中获取真实 IP。
     *
     * X-Forwarded-For 头的格式：客户端IP, 代理1IP, 代理2IP
     * 我们取第一个 IP，就是客户端的真实 IP。
     *
     * @param request HTTP 请求对象
     * @return 客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先从 X-Forwarded-For 头获取（Nginx 等反向代理设置）
        String ip = request.getHeader("X-Forwarded-For");
        // 如果头不存在或值为 "unknown"，尝试下一个头
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 从 X-Real-IP 头获取（Nginx 的 proxy_set_header X-Real-IP 设置）
            ip = request.getHeader("X-Real-IP");
        }
        // 如果还是获取不到，使用直接连接的远程地址
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP（经过多层代理），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
