package com.example.trigger;

import com.example.core.ExecutionEngine;
import com.example.model.ExecutionResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP触发器
 * 处理HTTP请求触发的函数调用
 */
@Component
public class HttpTrigger {
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    /**
     * 处理HTTP请求
     */
    public ExecutionResult handleRequest(String functionName, HttpServletRequest request,
                                       Map<String, Object> body) {
        
        // 构建输入参数
        Map<String, Object> input = new HashMap<>();
        
        // 添加HTTP相关信息
        Map<String, Object> httpInfo = new HashMap<>();
        httpInfo.put("method", request.getMethod());
        httpInfo.put("path", request.getRequestURI());
        httpInfo.put("queryString", request.getQueryString());
        httpInfo.put("remoteAddr", request.getRemoteAddr());
        httpInfo.put("userAgent", request.getHeader("User-Agent"));
        
        // 添加请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        httpInfo.put("headers", headers);
        
        // 添加查询参数
        Map<String, String[]> queryParams = request.getParameterMap();
        Map<String, Object> params = new HashMap<>();
        queryParams.forEach((key, values) -> {
            if (values.length == 1) {
                params.put(key, values[0]);
            } else {
                params.put(key, values);
            }
        });
        httpInfo.put("queryParams", params);
        
        input.put("http", httpInfo);
        
        // 添加请求体
        if (body != null) {
            input.put("body", body);
        }
        
        // 调用函数
        return executionEngine.invoke(functionName, input);
    }
    
    /**
     * 简化的GET请求处理
     */
    public ExecutionResult handleGetRequest(String functionName, HttpServletRequest request) {
        return handleRequest(functionName, request, null);
    }
    
    /**
     * 简化的POST请求处理
     */
    public ExecutionResult handlePostRequest(String functionName, HttpServletRequest request, 
                                           Map<String, Object> body) {
        return handleRequest(functionName, request, body);
    }
}