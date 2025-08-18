package com.example.sqltree;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service调用追踪切面
 * 自动追踪Service方法的调用，建立Service调用栈
 */
@Slf4j
@Aspect
@Component
public class ServiceCallTraceAspect {
    
    @Autowired
    private SqlCallTreeContext sqlCallTreeContext;
    
    /**
     * 拦截所有Service类的公共方法
     */
    @Around("execution(public * com.example.sqltree.*Service.*(..))")
    public Object traceServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取Service类名和方法名
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("Service切面拦截: {}.{}", serviceName, methodName);
        
        // 进入Service调用
        ServiceCallInfo serviceCall = sqlCallTreeContext.enterService(serviceName, methodName);
        log.info("Service调用进入: {}, 当前深度: {}", serviceName + "." + methodName, 
            serviceCall != null ? serviceCall.getDepth() : "null");
        
        try {
            // 执行原方法
            Object result = joinPoint.proceed();
            
            log.debug("Service方法执行成功: {}.{}", serviceName, methodName);
            return result;
            
        } catch (Throwable throwable) {
            log.error("Service方法执行失败: {}.{}, error: {}", 
                serviceName, methodName, throwable.getMessage());
            throw throwable;
            
        } finally {
            // 退出Service调用
            sqlCallTreeContext.exitService(serviceCall);
        }
    }
}