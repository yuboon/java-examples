package com.example.agent;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ControllerInterceptor {
    
    @RuntimeType
    public static Object intercept(
            @Origin Method method,
            @SuperCall Callable<?> callable,
            @AllArguments Object[] args) throws Exception {
        
        long startTime = System.currentTimeMillis();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        
        try {
            // 调用原方法
            return callable.call();
        } catch (Exception e) {
            // 记录异常信息
            MetricsCollector.recordException(className, methodName, e);
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            // 收集性能指标
            MetricsCollector.recordExecutionTime(className, methodName, executionTime);
        }
    }
}