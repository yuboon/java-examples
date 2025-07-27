package com.example.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 函数执行指标
 */
public class FunctionMetrics {
    
    private String functionName;
    private AtomicLong invocationCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);
    private AtomicLong errorCount = new AtomicLong(0);
    private AtomicLong totalExecutionTime = new AtomicLong(0);
    private AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private AtomicLong maxExecutionTime = new AtomicLong(0);
    private AtomicReference<LocalDateTime> lastInvocation = new AtomicReference<>();
    private AtomicReference<LocalDateTime> createTime = new AtomicReference<>(LocalDateTime.now());
    
    public FunctionMetrics(String functionName) {
        this.functionName = functionName;
    }
    
    // 记录函数调用
    public void recordInvocation(ExecutionResult result) {
        invocationCount.incrementAndGet();
        lastInvocation.set(LocalDateTime.now());
        
        if (result.isSuccess()) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }
        
        long executionTime = result.getExecutionTime();
        totalExecutionTime.addAndGet(executionTime);
        
        // 更新最小执行时间
        minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
        
        // 更新最大执行时间
        maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
    }
    
    // 获取平均执行时间
    public double getAvgExecutionTime() {
        long count = invocationCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime.get() / count;
    }
    
    // 获取成功率
    public double getSuccessRate() {
        long total = invocationCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount.get() / total * 100;
    }
    
    // 获取错误率
    public double getErrorRate() {
        return 100.0 - getSuccessRate();
    }
    
    // Getter方法
    public String getFunctionName() {
        return functionName;
    }
    
    public long getInvocationCount() {
        return invocationCount.get();
    }
    
    public long getSuccessCount() {
        return successCount.get();
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
    
    public long getTotalExecutionTime() {
        return totalExecutionTime.get();
    }
    
    public long getMinExecutionTime() {
        long min = minExecutionTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    public long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }
    
    public LocalDateTime getLastInvocation() {
        return lastInvocation.get();
    }
    
    public LocalDateTime getCreateTime() {
        return createTime.get();
    }
    
    @Override
    public String toString() {
        return "FunctionMetrics{" +
                "functionName='" + functionName + '\'' +
                ", invocationCount=" + invocationCount.get() +
                ", successCount=" + successCount.get() +
                ", errorCount=" + errorCount.get() +
                ", avgExecutionTime=" + String.format("%.2f", getAvgExecutionTime()) +
                ", successRate=" + String.format("%.2f", getSuccessRate()) + "%" +
                '}';
    }
}