package com.example.dashboard.aspect;

import com.example.dashboard.model.HierarchicalMethodMetrics;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class MethodMetricsAspect {
    
    private final ConcurrentHashMap<String, HierarchicalMethodMetrics> metricsMap = new ConcurrentHashMap<>();

    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public Object recordMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = buildMethodName(joinPoint);
        long startTime = System.nanoTime();
        boolean success = true;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
            
            metricsMap.computeIfAbsent(methodName, HierarchicalMethodMetrics::new)
                     .record(duration, success);
                     
            if (log.isDebugEnabled()) {
                log.debug("Method {} executed in {}ms, success: {}", methodName, duration, success);
            }
        }
    }

    private String buildMethodName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName + "()";
    }

    public Map<String, HierarchicalMethodMetrics> getMetricsSnapshot() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    public void clearMetrics() {
        metricsMap.clear();
        log.info("All metrics cleared");
    }

    public void removeStaleMetrics(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        metricsMap.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getLastAccessTime() > maxAgeMs);
    }
}