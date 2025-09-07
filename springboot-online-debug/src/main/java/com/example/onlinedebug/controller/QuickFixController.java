package com.example.onlinedebug.controller;

import com.example.onlinedebug.agent.DebugConfigManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 快速修复控制器 - 用于解决当前的调试规则问题
 */
@RestController
@RequestMapping("/api/quick-fix")
public class QuickFixController {
    
    /**
     * 一键设置正确的调试规则
     */
    @PostMapping("/setup-demo-debug")
    public Map<String, Object> setupDemoDebug() {
        System.out.println("[QUICK-FIX] Starting setup demo debug...");
        
        try {
            System.out.println("[QUICK-FIX] Step 1: Clearing all rules...");
            // 1. 清除所有规则
            DebugConfigManager.clearAllRules();
            
            System.out.println("[QUICK-FIX] Step 2: Adding method debug rules...");
            // 2. 添加正确的调试规则
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoService.getUserById");
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoController.getUser");
            
            System.out.println("[QUICK-FIX] Step 3: Retransforming classes...");
            // 3. 重新转换相关类
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoService");
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoController");
            
            System.out.println("[QUICK-FIX] Step 4: Getting status...");
            // 4. 返回当前状态
            DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
            
            System.out.println("[QUICK-FIX] Setup completed successfully!");
            
            return Map.of(
                "success", true,
                "message", "Demo debug rules setup successfully",
                "status", Map.of(
                    "exactMethodCount", status.getExactMethodCount(),
                    "totalRuleCount", status.getTotalRuleCount()
                ),
                "rules", Map.of(
                    "methods", java.util.Arrays.asList(
                        "com.example.onlinedebug.demo.DemoService.getUserById",
                        "com.example.onlinedebug.demo.DemoController.getUser"
                    )
                )
            );
        } catch (Exception e) {
            System.err.println("[QUICK-FIX] Error in setup: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Failed to setup debug rules: " + e.getMessage()
            );
        }
    }
    
    /**
     * 检查当前规则状态
     */
    @PostMapping("/check-rules")
    public Map<String, Object> checkRules() {
        try {
            String testMethod = "com.example.onlinedebug.demo.DemoService.getUserById";
            boolean shouldDebug = DebugConfigManager.shouldDebug(testMethod);
            
            DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
            
            return Map.of(
                "success", true,
                "testMethod", testMethod,
                "shouldDebugTestMethod", shouldDebug,
                "status", Map.of(
                    "exactMethodCount", status.getExactMethodCount(),
                    "classCount", status.getClassCount(),
                    "packageCount", status.getPackageCount(),
                    "totalRuleCount", status.getTotalRuleCount()
                )
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Failed to check rules: " + e.getMessage()
            );
        }
    }
}