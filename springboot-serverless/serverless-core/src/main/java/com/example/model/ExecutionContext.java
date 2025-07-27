package com.example.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 函数执行上下文
 */
public class ExecutionContext {
    
    private String requestId;
    private String functionName;
    private String functionVersion;
    private LocalDateTime startTime;
    private long timeoutMs;
    private Map<String, Object> environment;
    private Map<String, Object> attributes;
    
    public ExecutionContext(String requestId, String functionName) {
        this.requestId = requestId;
        this.functionName = functionName;
        this.functionVersion = "1.0";
        this.startTime = LocalDateTime.now();
        this.timeoutMs = 30000; // 默认30秒超时
        this.environment = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
    }
    
    // 获取剩余执行时间
    public long getRemainingTimeMs() {
        long elapsed = System.currentTimeMillis() - 
            java.sql.Timestamp.valueOf(startTime).getTime();
        return Math.max(0, timeoutMs - elapsed);
    }
    
    // 检查是否超时
    public boolean isTimeout() {
        return getRemainingTimeMs() <= 0;
    }
    
    // 设置环境变量
    public void setEnvironment(String key, Object value) {
        environment.put(key, value);
    }
    
    // 获取环境变量
    public Object getEnvironment(String key) {
        return environment.get(key);
    }
    
    // 设置属性
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    // 获取属性
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    // Getter和Setter方法
    public String getRequestId() {
        return requestId;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public String getFunctionVersion() {
        return functionVersion;
    }
    
    public void setFunctionVersion(String functionVersion) {
        this.functionVersion = functionVersion;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    @Override
    public String toString() {
        return "ExecutionContext{" +
                "requestId='" + requestId + '\'' +
                ", functionName='" + functionName + '\'' +
                ", functionVersion='" + functionVersion + '\'' +
                ", startTime=" + startTime +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}