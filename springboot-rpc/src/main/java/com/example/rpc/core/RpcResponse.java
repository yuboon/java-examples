package com.example.rpc.core;

public class RpcResponse {
    private String requestId;
    private Object result;
    private String error;
    private boolean success;
    
    public RpcResponse() {}
    
    public RpcResponse(String requestId) {
        this.requestId = requestId;
    }
    
    // getter/setter方法
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public Object getResult() { return result; }
    public void setResult(Object result) { 
        this.result = result; 
        this.success = true;
    }
    
    public String getError() { return error; }
    public void setError(String error) { 
        this.error = error; 
        this.success = false;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}