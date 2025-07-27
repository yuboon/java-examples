package com.example.functions;

import com.example.model.ExecutionContext;
import com.example.model.ServerlessFunction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello World示例函数
 */
public class HelloWorldFunction implements ServerlessFunction {
    
    @Override
    public Object handle(Map<String, Object> input, ExecutionContext context) throws Exception {
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello from Serverless Engine!");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("requestId", context.getRequestId());
        result.put("functionName", context.getFunctionName());
        result.put("input", input);
        
        // 模拟一些处理时间
        Thread.sleep(100);
        
        return result;
    }
}