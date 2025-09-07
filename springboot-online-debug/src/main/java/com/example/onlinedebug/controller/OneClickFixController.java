package com.example.onlinedebug.controller;

import com.example.onlinedebug.agent.DebugConfigManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 一键修复控制器
 */
@RestController
@RequestMapping("/api/one-click-fix")
public class OneClickFixController {
    
    /**
     * 一键修复所有调试规则问题
     */
    @PostMapping("/fix-all")
    public Map<String, Object> fixAll() {
        try {
            System.out.println("[ONE-CLICK-FIX] Starting comprehensive fix...");
            
            // 1. 完全清除所有规则
            DebugConfigManager.clearAllRules();
            System.out.println("[ONE-CLICK-FIX] Cleared all rules");
            
            // 2. 添加正确的演示调试规则
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoService.getUserById");
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoController.getUser");
            System.out.println("[ONE-CLICK-FIX] Added correct demo rules");
            
            // 3. 重新转换相关类
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoService");
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoController");
            System.out.println("[ONE-CLICK-FIX] Retransformed demo classes");
            
            // 4. 验证最终状态
            DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
            boolean shouldDebugService = DebugConfigManager.shouldDebug("com.example.onlinedebug.demo.DemoService.getUserById");
            boolean shouldDebugController = DebugConfigManager.shouldDebug("com.example.onlinedebug.demo.DemoController.getUser");
            
            System.out.println("[ONE-CLICK-FIX] Verification - DemoService should debug: " + shouldDebugService);
            System.out.println("[ONE-CLICK-FIX] Verification - DemoController should debug: " + shouldDebugController);
            
            return Map.of(
                "success", true,
                "message", "All debug rules fixed successfully",
                "verification", Map.of(
                    "shouldDebugService", shouldDebugService,
                    "shouldDebugController", shouldDebugController,
                    "totalRules", status.getTotalRuleCount()
                )
            );
            
        } catch (Exception e) {
            System.err.println("[ONE-CLICK-FIX] Error: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "Fix failed: " + e.getMessage()
            );
        }
    }
}