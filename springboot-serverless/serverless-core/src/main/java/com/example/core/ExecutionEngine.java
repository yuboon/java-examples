package com.example.core;

import com.example.executor.FunctionExecutor;
import com.example.model.ExecutionContext;
import com.example.model.ExecutionResult;
import com.example.model.FunctionMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 执行引擎
 * 负责函数的调用和执行管理
 */
@Component
@Slf4j
public class ExecutionEngine {
    
    @Autowired
    private FunctionManager functionManager;
    
    @Autowired
    private FunctionExecutor functionExecutor;
    
    /**
     * 调用函数
     */
    public ExecutionResult invoke(String functionName, Map<String, Object> input) {
        return invoke(functionName, input, null);
    }
    
    /**
     * 调用函数（带自定义上下文）
     */
    public ExecutionResult invoke(String functionName, Map<String, Object> input, 
                                ExecutionContext customContext) {
        
        // 检查函数是否存在
        if (!functionManager.functionExists(functionName)) {
            ExecutionResult result = new ExecutionResult(UUID.randomUUID().toString(), functionName);
            result.markFailure("FUNCTION_NOT_FOUND", "Function not found: " + functionName);
            return result;
        }
        
        // 获取函数定义
        FunctionManager.FunctionDefinition function = functionManager.getFunction(functionName);
        
        // 创建执行上下文
        ExecutionContext context = customContext;
        if (context == null) {
            context = new ExecutionContext(UUID.randomUUID().toString(), functionName);
        }
        
        // 设置函数配置
        context.setTimeoutMs(function.getTimeoutMs());
        
        // 设置环境变量
        function.getEnvironment().forEach(context::setEnvironment);
        
        try {
            // 执行函数
            ExecutionResult result = functionExecutor.execute(
                functionName,
                function.getJarPath(),
                function.getClassName(),
                input,
                context
            );
            
            // 记录指标
            FunctionMetrics metrics = functionManager.getFunctionMetrics(functionName);
            if (metrics != null) {
                metrics.recordInvocation(result);
            }
            
            // 打印执行日志
            System.out.printf("Function executed: %s, success: %s, time: %dms%n",
                functionName, result.isSuccess(), result.getExecutionTime());
            
            return result;
            
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            ExecutionResult result = new ExecutionResult(context.getRequestId(), functionName);
            result.markFailure("EXECUTION_ERROR", e.getMessage());
            
            // 记录指标
            FunctionMetrics metrics = functionManager.getFunctionMetrics(functionName);
            if (metrics != null) {
                metrics.recordInvocation(result);
            }
            
            return result;
        }
    }
    
    /**
     * 异步调用函数
     */
    public void invokeAsync(String functionName, Map<String, Object> input, 
                          AsyncCallback callback) {
        
        new Thread(() -> {
            ExecutionResult result = invoke(functionName, input);
            if (callback != null) {
                callback.onComplete(result);
            }
        }).start();
    }
    
    /**
     * 异步回调接口
     */
    @FunctionalInterface
    public interface AsyncCallback {
        void onComplete(ExecutionResult result);
    }
}