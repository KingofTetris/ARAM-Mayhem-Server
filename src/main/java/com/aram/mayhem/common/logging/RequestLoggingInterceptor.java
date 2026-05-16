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
 * 功能：记录每个 HTTP 请求的方法、URI、客户端 IP、响应状态、耗时
 * 日志级别：INFO（正常）、WARN（4xx）、ERROR（5xx/异常）
 * 注册：WebMvcConfig 中注册，拦截 /api/**，排除 /api/auth/**
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = getClientIp(request);

        log.info(">>> {} {}{} | client={}", method, uri,
                query != null ? "?" + query : "", clientIp);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        if (ex != null) {
            log.error("<<< {} {} | status={} | time={}ms | error={}", method, uri, status, duration, ex.getMessage());
        } else if (status >= 500) {
            log.error("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        } else if (status >= 400) {
            log.warn("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        } else {
            log.info("<<< {} {} | status={} | time={}ms", method, uri, status, duration);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}