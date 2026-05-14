package com.aram.mayhem.common.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @PostConstruct
    public void init() {
        log.info("ServiceLoggingAspect initialized");
    }

    @Pointcut("execution(* com.aram.mayhem.service.impl..*.*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug(">> {}.{}()", className, methodName);

        try {
            Object result = joinPoint.proceed();
            log.debug("<< {}.{}() => success", className, methodName);
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("!! {}.{}() => bad request: {}", className, methodName, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("!! {}.{}() => conflict: {}", className, methodName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("!! {}.{}() => error: {}", className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}