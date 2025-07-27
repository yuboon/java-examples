package com.example.api;

import com.example.core.ExecutionEngine;
import com.example.core.FunctionManager;
import com.example.model.ExecutionResult;
import com.example.model.FunctionMetrics;
import com.example.trigger.HttpTrigger;
import com.example.trigger.TimerTrigger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Serverless API控制器
 */
@RestController
@RequestMapping("/serverless")
public class ServerlessController {
    
    @Autowired
    private FunctionManager functionManager;
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    @Autowired
    private HttpTrigger httpTrigger;
    
    @Autowired
    private TimerTrigger timerTrigger;
    
    /**
     * 调用函数
     */
    @PostMapping("/functions/{functionName}/invoke")
    public ResponseEntity<Map<String, Object>> invokeFunction(
            @PathVariable String functionName,
            @RequestBody(required = false) Map<String, Object> input,
            HttpServletRequest request) {
        
        ExecutionResult result = httpTrigger.handlePostRequest(functionName, request, input);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", result.getRequestId());
        response.put("functionName", result.getFunctionName());
        response.put("success", result.isSuccess());
        response.put("executionTime", result.getExecutionTime());

        if (result.isSuccess()) {
            response.put("result", result.getResult());
        } else {
            response.put("errorType", result.getErrorType());
            response.put("errorMessage", result.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET方式调用函数
     */
    @GetMapping("/functions/{functionName}/invoke")
    public ResponseEntity<Map<String, Object>> invokeFunctionGet(
            @PathVariable String functionName,
            HttpServletRequest request) {
        
        ExecutionResult result = httpTrigger.handleGetRequest(functionName, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", result.getRequestId());
        response.put("functionName", result.getFunctionName());
        response.put("success", result.isSuccess());
        response.put("executionTime", result.getExecutionTime());
        
        if (result.isSuccess()) {
            response.put("result", result.getResult());
        } else {
            response.put("errorType", result.getErrorType());
            response.put("errorMessage", result.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 注册函数
     */
    @PostMapping("/functions/{functionName}")
    public ResponseEntity<Map<String, String>> registerFunction(
            @PathVariable String functionName,
            @RequestBody Map<String, Object> config) {
        
        String jarPath = (String) config.get("jarPath");
        String className = (String) config.get("className");
        Long timeoutMs = config.containsKey("timeoutMs") ? 
            ((Number) config.get("timeoutMs")).longValue() : 30000L;

        @SuppressWarnings("unchecked")
        Map<String, Object> environment = (Map<String, Object>) config.get("environment");
        
        functionManager.registerFunction(functionName, jarPath, className, 
                                       timeoutMs, environment);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Function registered successfully");
        response.put("functionName", functionName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有函数列表
     */
    @GetMapping("/functions")
    public ResponseEntity<Map<String, Object>> getAllFunctions() {
        Collection<FunctionManager.FunctionDefinition> functions = functionManager.getAllFunctions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("functions", functions);
        response.put("count", functions.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取函数详情
     */
    @GetMapping("/functions/{functionName}")
    public ResponseEntity<FunctionManager.FunctionDefinition> getFunctionDetail(
            @PathVariable String functionName) {
        
        FunctionManager.FunctionDefinition function = functionManager.getFunction(functionName);
        if (function == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(function);
    }
    
    /**
     * 删除函数
     */
    @DeleteMapping("/functions/{functionName}")
    public ResponseEntity<Map<String, String>> deleteFunction(@PathVariable String functionName) {
        functionManager.removeFunction(functionName);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Function deleted successfully");
        response.put("functionName", functionName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取函数指标
     */
    @GetMapping("/functions/{functionName}/metrics")
    public ResponseEntity<FunctionMetrics> getFunctionMetrics(@PathVariable String functionName) {
        FunctionMetrics metrics = functionManager.getFunctionMetrics(functionName);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取所有函数指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Collection<FunctionMetrics> metrics = functionManager.getAllMetrics();
        
        Map<String, Object> response = new HashMap<>();
        response.put("metrics", metrics);
        response.put("count", metrics.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 注册定时任务
     */
    @PostMapping("/timer-tasks/{taskName}")
    public ResponseEntity<Map<String, String>> registerTimerTask(
            @PathVariable String taskName,
            @RequestBody Map<String, String> config) {
        
        String functionName = config.get("functionName");
        String cronExpression = config.get("cronExpression");
        
        timerTrigger.registerTimerTask(taskName, functionName, cronExpression);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Timer task registered successfully");
        response.put("taskName", taskName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有定时任务
     */
    @GetMapping("/timer-tasks")
    public ResponseEntity<Map<String, Object>> getAllTimerTasks() {
        Map<String, TimerTrigger.TimerTask> tasks = timerTrigger.getAllTimerTasks();
        
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", tasks);
        response.put("count", tasks.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 手动执行定时任务
     */
    @PostMapping("/timer-tasks/{taskName}/execute")
    public ResponseEntity<Map<String, Object>> executeTimerTask(@PathVariable String taskName) {
        ExecutionResult result = timerTrigger.executeTimerTask(taskName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", result.getRequestId());
        response.put("success", result.isSuccess());
        response.put("executionTime", result.getExecutionTime());
        
        if (result.isSuccess()) {
            response.put("result", result.getResult());
        } else {
            response.put("errorType", result.getErrorType());
            response.put("errorMessage", result.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 系统信息
        Runtime runtime = Runtime.getRuntime();
        status.put("totalMemory", runtime.totalMemory());
        status.put("freeMemory", runtime.freeMemory());
        status.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        status.put("maxMemory", runtime.maxMemory());
        status.put("availableProcessors", runtime.availableProcessors());
        
        // 函数统计
        status.put("functionCount", functionManager.getFunctionCount());
        status.put("timerTaskCount", timerTrigger.getAllTimerTasks().size());
        
        // 总执行次数
        long totalInvocations = functionManager.getAllMetrics().stream()
                .mapToLong(FunctionMetrics::getInvocationCount)
                .sum();
        status.put("totalInvocations", totalInvocations);
        
        return ResponseEntity.ok(status);
    }
}