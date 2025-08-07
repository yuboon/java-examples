package com.example.controller;

import com.example.service.DynamicLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 动态日志级别管理控制器
 * 提供REST API接口管理和测试日志级别
 */
@RestController
@RequestMapping("/api/logging")
@Slf4j
public class LoggingController {

    @Autowired
    private DynamicLoggingService dynamicLoggingService;

    /**
     * 设置日志级别
     */
    @PostMapping("/level/{loggerName}")
    public ResponseEntity<Map<String, Object>> setLogLevel(
            @PathVariable String loggerName,
            @RequestParam String level) {
        try {
            dynamicLoggingService.setLogLevel(loggerName, level);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "日志级别设置成功",
                "loggerName", loggerName,
                "newLevel", level,
                "currentLevel", dynamicLoggingService.getLogLevel(loggerName)
            ));
        } catch (Exception e) {
            log.error("设置日志级别失败: {} = {}", loggerName, level, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "设置日志级别失败: " + e.getMessage(),
                "loggerName", loggerName,
                "requestedLevel", level
            ));
        }
    }

    /**
     * 获取日志级别
     */
    @GetMapping("/level/{loggerName}")
    public ResponseEntity<Map<String, Object>> getLogLevel(@PathVariable String loggerName) {
        try {
            String currentLevel = dynamicLoggingService.getLogLevel(loggerName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "loggerName", loggerName,
                "currentLevel", currentLevel
            ));
        } catch (Exception e) {
            log.error("获取日志级别失败: {}", loggerName, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取日志级别失败: " + e.getMessage(),
                "loggerName", loggerName
            ));
        }
    }

    /**
     * 获取所有已配置的日志级别
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, Object>> getAllLogLevels() {
        try {
            Map<String, String> logLevels = dynamicLoggingService.getAllLogLevels();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "获取所有日志级别成功",
                "logLevels", logLevels,
                "count", logLevels.size()
            ));
        } catch (Exception e) {
            log.error("获取所有日志级别失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取所有日志级别失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量设置日志级别
     */
    @PostMapping("/levels")
    public ResponseEntity<Map<String, Object>> setLogLevels(@RequestBody Map<String, String> logLevels) {
        try {
            dynamicLoggingService.setLogLevels(logLevels);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量设置日志级别成功",
                "processedCount", logLevels.size(),
                "logLevels", logLevels
            ));
        } catch (Exception e) {
            log.error("批量设置日志级别失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "批量设置日志级别失败: " + e.getMessage(),
                "requestedLevels", logLevels
            ));
        }
    }

    /**
     * 重置日志级别为默认值
     */
    @DeleteMapping("/level/{loggerName}")
    public ResponseEntity<Map<String, Object>> resetLogLevel(@PathVariable String loggerName) {
        try {
            dynamicLoggingService.resetLogLevel(loggerName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "日志级别重置成功",
                "loggerName", loggerName,
                "newLevel", dynamicLoggingService.getLogLevel(loggerName)
            ));
        } catch (Exception e) {
            log.error("重置日志级别失败: {}", loggerName, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "重置日志级别失败: " + e.getMessage(),
                "loggerName", loggerName
            ));
        }
    }

    /**
     * 测试日志输出
     */
    @PostMapping("/test/{loggerName}")
    public ResponseEntity<Map<String, Object>> testLogOutput(@PathVariable String loggerName) {
        try {
            Map<String, Object> result = dynamicLoggingService.testLogOutput(loggerName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "日志测试完成",
                "testResult", result
            ));
        } catch (Exception e) {
            log.error("测试日志输出失败: {}", loggerName, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "测试日志输出失败: " + e.getMessage(),
                "loggerName", loggerName
            ));
        }
    }

    /**
     * 获取支持的日志级别列表
     */
    @GetMapping("/supported-levels")
    public ResponseEntity<Map<String, Object>> getSupportedLogLevels() {
        try {
            String[] supportedLevels = dynamicLoggingService.getSupportedLogLevels();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "获取支持的日志级别成功",
                "supportedLevels", supportedLevels
            ));
        } catch (Exception e) {
            log.error("获取支持的日志级别失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取支持的日志级别失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 快速设置常用日志级别
     */
    @PostMapping("/quick-set")
    public ResponseEntity<Map<String, Object>> quickSetLogLevels(
            @RequestParam(required = false, defaultValue = "INFO") String rootLevel,
            @RequestParam(required = false, defaultValue = "DEBUG") String businessLevel,
            @RequestParam(required = false, defaultValue = "WARN") String frameworkLevel) {
        try {
            // 设置根日志级别
            dynamicLoggingService.setLogLevel("ROOT", rootLevel);
            
            // 设置业务包日志级别
            dynamicLoggingService.setLogLevel("com.example", businessLevel);
            
            // 设置框架日志级别
            dynamicLoggingService.setLogLevel("org.springframework", frameworkLevel);
            dynamicLoggingService.setLogLevel("com.zaxxer.hikari", frameworkLevel);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "快速设置日志级别成功",
                "settings", Map.of(
                    "ROOT", rootLevel,
                    "com.example", businessLevel,
                    "org.springframework", frameworkLevel,
                    "com.zaxxer.hikari", frameworkLevel
                )
            ));
        } catch (Exception e) {
            log.error("快速设置日志级别失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "快速设置日志级别失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 日志管理状态检查
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLoggingStatus() {
        try {
            Map<String, String> allLevels = dynamicLoggingService.getAllLogLevels();
            String[] supportedLevels = dynamicLoggingService.getSupportedLogLevels();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "日志管理状态正常",
                "totalLoggers", allLevels.size(),
                "supportedLevels", supportedLevels,
                "serviceAvailable", true,
                "currentLoggers", allLevels
            ));
        } catch (Exception e) {
            log.error("获取日志管理状态失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取日志管理状态失败: " + e.getMessage(),
                "serviceAvailable", false
            ));
        }
    }
}