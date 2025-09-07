package com.example.onlinedebug.controller;

import com.example.onlinedebug.agent.DebugConfigManager;
import com.example.onlinedebug.service.OnlineDebugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 在线调试控制器
 * 提供REST接口管理调试规则
 */
@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class OnlineDebugController {
    
    @Autowired
    private OnlineDebugService onlineDebugService;
    
    /**
     * 获取调试规则状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", Map.of(
                "exactMethodCount", status.getExactMethodCount(),
                "patternCount", status.getPatternCount(),
                "classCount", status.getClassCount(),
                "packageCount", status.getPackageCount(),
                "globalEnabled", status.isGlobalEnabled(),
                "totalRuleCount", status.getTotalRuleCount()
            ));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 添加方法级别调试
     */
    @PostMapping("/method")
    public ResponseEntity<Map<String, Object>> addMethodDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.addMethodDebug(request.getTarget());
            
            // 提取类名并重新转换
            String className = extractClassName(request.getTarget());
            if (className != null) {
                com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass(className);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Method debug rule added: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 移除方法级别调试
     */
    @DeleteMapping("/method")
    public ResponseEntity<Map<String, Object>> removeMethodDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.removeMethodDebug(request.getTarget());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Method debug rule removed: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 添加类级别调试
     */
    @PostMapping("/class")
    public ResponseEntity<Map<String, Object>> addClassDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.addClassDebug(request.getTarget());
            
            // 重新转换该类
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass(request.getTarget());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Class debug rule added: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 移除类级别调试
     */
    @DeleteMapping("/class")
    public ResponseEntity<Map<String, Object>> removeClassDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.removeClassDebug(request.getTarget());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Class debug rule removed: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 添加包级别调试
     */
    @PostMapping("/package")
    public ResponseEntity<Map<String, Object>> addPackageDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.addPackageDebug(request.getTarget());
            
            // 重新转换包下的所有类
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformPackage(request.getTarget());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Package debug rule added: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 移除包级别调试
     */
    @DeleteMapping("/package")
    public ResponseEntity<Map<String, Object>> removePackageDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.removePackageDebug(request.getTarget());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Package debug rule removed: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 添加模式匹配调试规则
     */
    @PostMapping("/pattern")
    public ResponseEntity<Map<String, Object>> addPatternDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.addPatternDebug(request.getTarget());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pattern debug rule added: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 移除模式匹配调试规则
     */
    @DeleteMapping("/pattern")
    public ResponseEntity<Map<String, Object>> removePatternDebug(@RequestBody DebugRuleRequest request) {
        try {
            DebugConfigManager.removePatternDebug(request.getTarget());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pattern debug rule removed: " + request.getTarget()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 设置全局调试开关
     */
    @PostMapping("/global")
    public ResponseEntity<Map<String, Object>> setGlobalDebug(@RequestBody GlobalDebugRequest request) {
        try {
            DebugConfigManager.setGlobalDebug(request.isEnabled());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Global debug " + (request.isEnabled() ? "enabled" : "disabled")
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 清除所有调试规则
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllRules() {
        try {
            DebugConfigManager.clearAllRules();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All debug rules cleared"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取已加载的类信息（用于前端选择）
     */
    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getLoadedClasses() {
        try {
            Map<String, Object> result = onlineDebugService.getLoadedClasses();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取指定类的方法信息
     */
    @GetMapping("/methods/{className}")
    public ResponseEntity<Map<String, Object>> getClassMethods(@PathVariable String className) {
        try {
            Map<String, Object> result = onlineDebugService.getClassMethods(className);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 从完整方法名中提取类名
     */
    private String extractClassName(String fullMethodName) {
        int lastDotIndex = fullMethodName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fullMethodName.substring(0, lastDotIndex);
        }
        return null;
    }
    
    /**
     * 调试规则请求对象
     */
    public static class DebugRuleRequest {
        private String target;
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }
    
    /**
     * 全局调试请求对象
     */
    public static class GlobalDebugRequest {
        private boolean enabled;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * 紧急修复 - 直接清除并设置正确的调试规则
     */
    @PostMapping("/emergency-fix")
    public ResponseEntity<Map<String, Object>> emergencyFix() {
        System.out.println("[EMERGENCY-FIX] Starting emergency fix...");
        
        try {
            System.out.println("[EMERGENCY-FIX] Step 1: Clearing all rules...");
            DebugConfigManager.clearAllRules();
            
            System.out.println("[EMERGENCY-FIX] Step 2: Adding correct method debug rules...");
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoService.getUserById");
            DebugConfigManager.addMethodDebug("com.example.onlinedebug.demo.DemoController.getUser");
            
            System.out.println("[EMERGENCY-FIX] Step 3: Retransforming classes...");
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoService");
            com.example.onlinedebug.agent.DynamicRetransformManager.retransformClass("com.example.onlinedebug.demo.DemoController");
            
            // 验证规则是否添加成功
            boolean shouldDebug = DebugConfigManager.shouldDebug("com.example.onlinedebug.demo.DemoService.getUserById");
            System.out.println("[EMERGENCY-FIX] Verification - shouldDebug for DemoService.getUserById: " + shouldDebug);
            
            DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
            System.out.println("[EMERGENCY-FIX] Final status - exact methods: " + status.getExactMethodCount());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Emergency fix completed",
                "shouldDebugDemoService", shouldDebug,
                "exactMethodCount", status.getExactMethodCount()
            ));
        } catch (Exception e) {
            System.err.println("[EMERGENCY-FIX] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Emergency fix failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取调试规则详细列表
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getRules() {
        try {
            Map<String, Object> result = onlineDebugService.getDebugRules();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}