package com.example.agent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MetricsCollector {
    
    private static final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private static final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private static final Map<String, Counter> exceptionCounters = new ConcurrentHashMap<>();
    
    public static void recordExecutionTime(String className, String methodName, long executionTime) {
        String key = className + "." + methodName;
        getOrCreateTimer(key, "controller").record(executionTime, TimeUnit.MILLISECONDS);
        System.out.printf("Controller执行: %s, 耗时: %d ms%n", key, executionTime);
    }
    
    public static void recordException(String className, String methodName, Exception e) {
        String key = className + "." + methodName;
        getOrCreateExceptionCounter(key, "controller", e.getClass().getSimpleName()).increment();
        System.out.printf("Controller异常: %s, 异常类型: %s, 消息: %s%n", 
                key, e.getClass().getName(), e.getMessage());
    }
    
    public static void recordSqlExecutionTime(String className, String methodName, long executionTime) {
        String key = className + "." + methodName;
        getOrCreateTimer(key, "sql").record(executionTime, TimeUnit.MILLISECONDS);
        System.out.printf("SQL执行: %s, 耗时: %d ms%n", key, executionTime);
    }
    
    public static void recordSqlException(String className, String methodName, Exception e) {
        String key = className + "." + methodName;
        getOrCreateExceptionCounter(key, "sql", e.getClass().getSimpleName()).increment();
        System.out.printf("SQL异常: %s, 异常类型: %s, 消息: %s%n", 
                key, e.getClass().getName(), e.getMessage());
    }
    
    private static Timer getOrCreateTimer(String name, String type) {
        return timers.computeIfAbsent(name, k -> 
            Timer.builder("app.execution.time")
                .tag("name", name)
                .tag("type", type)
                .register(registry)
        );
    }
    
    private static Counter getOrCreateExceptionCounter(String name, String type, String exceptionType) {
        String key = name + "." + exceptionType;
        return exceptionCounters.computeIfAbsent(key, k -> 
            Counter.builder("app.exception.count")
                .tag("name", name)
                .tag("type", type)
                .tag("exception", exceptionType)
                .register(registry)
        );
    }

    public static void monitorJvmMetrics() {
        // 注册JVM内存指标
        new JvmMemoryMetrics().bindTo(registry);
        // 注册GC指标
        new JvmGcMetrics().bindTo(registry);
        // 注册线程指标
        new JvmThreadMetrics().bindTo(registry);
    }

    // 获取Prometheus格式的指标数据
    public static String scrape() {
        return registry.scrape();
    }
    
    // 获取注册表，可以被其他组件使用
    public static MeterRegistry getRegistry() {
        return registry;
    }
}