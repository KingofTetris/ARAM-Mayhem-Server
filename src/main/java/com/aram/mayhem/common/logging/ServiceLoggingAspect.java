package com.aram.mayhem.common.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Service 层日志切面
 *
 * AOP（面向切面编程）是一种编程思想，可以在不修改原有代码的情况下，给方法"加料"。
 * 这个切面就像一个"隐形监工"，自动监控所有 Service 方法的执行情况。
 *
 * 它做了什么？
 * - 在 Service 方法执行前：记录"方法开始执行"
 * - 在 Service 方法执行后：记录"方法执行成功"
 * - 在 Service 方法抛异常时：记录"方法执行失败"和异常信息
 *
 * 与 RequestLoggingInterceptor 的分工：
 * - RequestLoggingInterceptor：只记录 Controller 层（HTTP 请求级别）
 * - ServiceLoggingAspect：记录 Service 层（业务逻辑级别）
 * - 两者配合，可以追踪一个请求从进入到完成的完整链路
 *
 * 日志级别策略：
 * - 正常执行：DEBUG 级别（信息量大，只在需要详细调试时开启）
 * - 参数/状态异常：WARN 级别（IllegalArgumentException、IllegalStateException）
 * - 未知异常：ERROR 级别（记录完整堆栈，方便排查）
 *
 * 拦截范围：
 * com.aram.mayhem.service.impl 包下的所有类的所有方法
 *
 * 关联类：
 * - RequestLoggingInterceptor：Controller 层日志拦截器
 *
 * @see RequestLoggingInterceptor
 */
@Aspect // 声明这是一个 AOP 切面类
@Component // 注册为 Spring 组件
public class ServiceLoggingAspect {

    // SLF4J 日志记录器
    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    /**
     * 初始化方法 —— Spring Bean 创建后自动执行
     *
     * 用于确认切面已成功加载。如果启动日志中没有这行输出，
     * 说明切面配置有问题，AOP 可能没有生效。
     */
    @PostConstruct
    public void init() {
        log.info("ServiceLoggingAspect initialized");
    }

    /**
     * 定义切点 —— 指定要拦截哪些方法
     *
     * 切点表达式：execution(* com.aram.mayhem.service.impl..*.*(..))
     * 解读：
     * - execution()：匹配方法执行
     * - 第一个 *：匹配任意返回值类型
     * - com.aram.mayhem.service.impl..：匹配该包及其子包下的所有类
     * - 第二个 *：匹配所有类名
     * - 第三个 *：匹配所有方法名
     * - (..)：匹配任意参数列表
     *
     * 简单来说：拦截 service.impl 包下所有类的所有方法
     */
    @Pointcut("execution(* com.aram.mayhem.service.impl..*.*(..))")
    public void serviceLayer() {
        // 切点方法不需要实现，只是一个标识
    }

    /**
     * 环绕通知 —— 在方法执行前后都插入逻辑
     *
     * @Around 是最强大的通知类型，可以完全控制目标方法的执行：
     * - 可以在方法执行前做事情（如记录开始日志）
     * - 可以决定是否执行目标方法（joinPoint.proceed()）
     * - 可以修改目标方法的返回值
     * - 可以捕获和处理目标方法的异常
     *
     * @param joinPoint 连接点，包含目标方法的信息（类名、方法名、参数等）
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("serviceLayer()") // 引用上面定义的切点
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取目标类的简单名称（如 HeroServiceImpl）
        String className = joinPoint.getTarget().getClass().getSimpleName();
        // 获取目标方法的名称（如 getHeroDetail）
        String methodName = joinPoint.getSignature().getName();

        // 记录方法开始执行（DEBUG 级别，默认不输出，需要时调整日志级别）
        log.debug(">> {}.{}()", className, methodName);

        try {
            // 执行目标方法 —— 这行代码会实际调用 Service 方法
            // proceed() 的返回值就是目标方法的返回值
            Object result = joinPoint.proceed();
            // 方法执行成功，记录成功日志
            log.debug("<< {}.{}() => success", className, methodName);
            return result;
        } catch (IllegalArgumentException e) {
            // 参数不合法：如传入的 ID 为负数
            log.warn("!! {}.{}() => bad request: {}", className, methodName, e.getMessage());
            throw e; // 继续抛出，让 GlobalExceptionHandler 处理
        } catch (IllegalStateException e) {
            // 状态不合法：如重复操作
            log.warn("!! {}.{}() => conflict: {}", className, methodName, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 未知异常：可能是代码 Bug，记录完整堆栈
            log.error("!! {}.{}() => error: {}", className, methodName, e.getMessage(), e);
            throw e; // 继续抛出，让 GlobalExceptionHandler 处理
        }
    }
}
