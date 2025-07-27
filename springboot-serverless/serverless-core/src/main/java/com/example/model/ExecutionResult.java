package com.example.model;

import java.time.LocalDateTime;

/**
 * 函数执行结果
 */
public class ExecutionResult {
    
    private String requestId;
    private String functionName;
    private boolean success;
    private Object result;
    private String errorMessage;
    private String errorType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long executionTime;
    
    public ExecutionResult(String requestId, String functionName) {
        this.requestId = requestId;
        this.functionName = functionName;
        this.startTime = LocalDateTime.now();
    }
    
    // 标记执行成功
    public void markSuccess(Object result) {
        this.success = true;
        this.result = result;
        this.endTime = LocalDateTime.now();
        this.executionTime = calculateExecutionTime();
    }
    
    // 标记执行失败
    public void markFailure(String errorType, String errorMessage) {
        this.success = false;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        this.executionTime = calculateExecutionTime();
    }
    
    // 计算执行时间
    private long calculateExecutionTime() {
        if (startTime != null && endTime != null) {
            return java.sql.Timestamp.valueOf(endTime).getTime() - 
                   java.sql.Timestamp.valueOf(startTime).getTime();
        }
        return 0;
    }
    
    // Getter和Setter方法
    public String getRequestId() {
        return requestId;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Object getResult() {
        return result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }


    @Override
    public String toString() {
        return "ExecutionResult{" +
                "requestId='" + requestId + '\'' +
                ", functionName='" + functionName + '\'' +
                ", success=" + success +
                ", executionTime=" + executionTime +
                '}';
    }
}