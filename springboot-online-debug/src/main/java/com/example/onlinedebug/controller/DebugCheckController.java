package com.example.onlinedebug.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.onlinedebug.agent.DebugConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试状态检查控制器
 */
@RestController
@RequestMapping("/api/debug-check")
public class DebugCheckController {
    
    /**
     * 检查调试状态详情
     */
    @GetMapping("/detailed-status")
    public Map<String, Object> getDetailedStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取基本状态
        DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
        result.put("basicStatus", Map.of(
            "exactMethodCount", status.getExactMethodCount(),
            "patternCount", status.getPatternCount(),
            "classCount", status.getClassCount(),
            "packageCount", status.getPackageCount(),
            "globalEnabled", status.isGlobalEnabled(),
            "totalRuleCount", status.getTotalRuleCount()
        ));
        
        // 测试具体方法是否会被调试
        String testMethod = "com.example.onlinedebug.demo.DemoService.getUserById";
        boolean shouldDebug = DebugConfigManager.shouldDebug(testMethod);
        result.put("testMethod", testMethod);
        result.put("shouldDebugTestMethod", shouldDebug);
        
        // 测试类是否会被增强
        String testClass = "com.example.onlinedebug.demo.DemoService";
        boolean shouldEnhanceClass = DebugConfigManager.shouldDebugClass(testClass);
        result.put("testClass", testClass);
        result.put("shouldEnhanceClass", shouldEnhanceClass);
        
        return result;
    }
    
    /**
     * 测试方法调用 - 用于验证调试是否生效
     */
    @GetMapping("/test-call")
    public Map<String, Object> testCall() {
        System.out.println("[TEST] About to call demo service...");
        
        // 直接调用一个简单的方法进行测试
        String result = "Test completed at " + System.currentTimeMillis();
        
        System.out.println("[TEST] Test call completed: " + result);
        
        return Map.of(
            "success", true,
            "message", "Test call completed",
            "result", result
        );
    }
}