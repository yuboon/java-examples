package com.example.firewall.controller;

import com.example.firewall.entity.FirewallRule;
import com.example.firewall.entity.FirewallBlacklist;
import com.example.firewall.entity.FirewallWhitelist;
import com.example.firewall.entity.FirewallAccessLog;
import com.example.firewall.entity.FirewallStatistics;
import com.example.firewall.service.RuleManager;
import com.example.firewall.service.FirewallService;
import com.example.firewall.interceptor.FirewallInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 防火墙管理REST API控制器
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/firewall")
@CrossOrigin(origins = "*") // 允许跨域访问
public class FirewallController {
    
    @Autowired
    private RuleManager ruleManager;
    
    @Autowired
    private FirewallService firewallService;
    
    @Autowired
    private FirewallInterceptor firewallInterceptor;
    
    /**
     * 获取系统概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 今日统计
            Map<String, Object> todayStats = firewallService.getTodayOverview();
            overview.put("todayStats", todayStats);
            
            // 缓存统计
            Map<String, Object> cacheStats = ruleManager.getCacheStats();
            overview.put("cacheStats", cacheStats);
            
            // 拦截器缓存统计
            Map<String, Object> interceptorStats = firewallInterceptor.getCacheStats();
            overview.put("interceptorStats", interceptorStats);
            
            // 系统健康状态
            Map<String, Object> healthStatus = firewallService.getHealthStatus();
            overview.put("healthStatus", healthStatus);
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("获取系统概览失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取系统概览失败", e.getMessage()));
        }
    }
    
    // ==================== 防火墙规则管理 ====================
    
    /**
     * 获取所有防火墙规则
     */
    @GetMapping("/rules")
    public ResponseEntity<List<FirewallRule>> getAllRules() {
        try {
            List<FirewallRule> rules = ruleManager.getAllRules();
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            log.error("获取防火墙规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据ID获取防火墙规则
     */
    @GetMapping("/rules/{id}")
    public ResponseEntity<FirewallRule> getRuleById(@PathVariable Long id) {
        try {
            List<FirewallRule> rules = ruleManager.getAllRules();
            FirewallRule rule = rules.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            
            if (rule != null) {
                return ResponseEntity.ok(rule);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取防火墙规则失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建防火墙规则
     */
    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody FirewallRule rule) {
        try {
            rule.setId(null); // 确保是新建
            rule.setCreatedTime(LocalDateTime.now());
        rule.setUpdatedTime(LocalDateTime.now());
            
            boolean success = ruleManager.saveRule(rule);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("规则创建成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("规则创建失败", "保存规则时发生错误"));
            }
        } catch (Exception e) {
            log.error("创建防火墙规则失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("规则创建失败", e.getMessage()));
        }
    }
    
    /**
     * 更新防火墙规则
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody FirewallRule rule) {
        try {
            rule.setId(id);
            rule.setUpdatedTime(LocalDateTime.now());
            
            boolean success = ruleManager.saveRule(rule);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("规则更新成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("规则更新失败", "保存规则时发生错误"));
            }
        } catch (Exception e) {
            log.error("更新防火墙规则失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("规则更新失败", e.getMessage()));
        }
    }
    
    /**
     * 删除防火墙规则
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        try {
            boolean success = ruleManager.deleteRule(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("规则删除成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("规则删除失败", "删除规则时发生错误"));
            }
        } catch (Exception e) {
            log.error("删除防火墙规则失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("规则删除失败", e.getMessage()));
        }
    }

    /**
     * 切换防火墙规则启用状态
     */
    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRule(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Boolean enabled = (Boolean) request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("参数错误", "enabled参数不能为空"));
            }

            // 获取现有规则
            List<FirewallRule> rules = ruleManager.getAllRules();
            FirewallRule rule = rules.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (rule == null) {
                return ResponseEntity.notFound().build();
            }

            // 更新启用状态
            rule.setEnabled(enabled);
            rule.setUpdatedTime(LocalDateTime.now());
            
            boolean success = ruleManager.saveRule(rule);
            if (success) {
                String message = enabled ? "规则已启用" : "规则已禁用";
                return ResponseEntity.ok(createSuccessResponse(message));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("状态切换失败", "更新规则时发生错误"));
            }
        } catch (Exception e) {
            log.error("切换防火墙规则状态失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("状态切换失败", e.getMessage()));
        }
    }
    
    // ==================== 黑名单管理 ====================
    
    /**
     * 获取所有黑名单
     */
    @GetMapping("/blacklist")
    public ResponseEntity<List<FirewallBlacklist>> getAllBlacklist() {
        try {
            List<FirewallBlacklist> blacklist = ruleManager.getAllBlacklist();
            return ResponseEntity.ok(blacklist);
        } catch (Exception e) {
            log.error("获取黑名单失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 添加黑名单
     */
    @PostMapping("/blacklist")
    public ResponseEntity<Map<String, Object>> addBlacklist(@RequestBody FirewallBlacklist blacklist) {
        try {
            blacklist.setId(null);
            blacklist.setCreatedTime(LocalDateTime.now());
            blacklist.setUpdatedTime(LocalDateTime.now());
            // 确保enabled字段有默认值
            if (blacklist.getEnabled() == null) {
                blacklist.setEnabled(true);
            }
            
            boolean success = ruleManager.addBlacklist(blacklist);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("黑名单添加成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("黑名单添加失败", "保存黑名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("添加黑名单失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("黑名单添加失败", e.getMessage()));
        }
    }
    
    /**
     * 更新黑名单
     */
    @PutMapping("/blacklist/{id}")
    public ResponseEntity<Map<String, Object>> updateBlacklist(@PathVariable Long id, @RequestBody FirewallBlacklist blacklist) {
        try {
            blacklist.setId(id);
            blacklist.setUpdatedTime(LocalDateTime.now());
            
            boolean success = ruleManager.updateBlacklist(blacklist);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("黑名单更新成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("黑名单更新失败", "更新黑名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("更新黑名单失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("黑名单更新失败", e.getMessage()));
        }
    }
    
    /**
     * 删除黑名单
     */
    @DeleteMapping("/blacklist/{id}")
    public ResponseEntity<Map<String, Object>> deleteBlacklist(@PathVariable Long id) {
        try {
            boolean success = ruleManager.deleteBlacklist(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("黑名单删除成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("黑名单删除失败", "删除黑名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("删除黑名单失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("黑名单删除失败", e.getMessage()));
        }
    }
    
    // ==================== 白名单管理 ====================
    
    /**
     * 获取所有白名单
     */
    @GetMapping("/whitelist")
    public ResponseEntity<List<FirewallWhitelist>> getAllWhitelist() {
        try {
            List<FirewallWhitelist> whitelist = ruleManager.getAllWhitelist();
            return ResponseEntity.ok(whitelist);
        } catch (Exception e) {
            log.error("获取白名单失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 添加白名单
     */
    @PostMapping("/whitelist")
    public ResponseEntity<Map<String, Object>> addWhitelist(@RequestBody FirewallWhitelist whitelist) {
        try {
            whitelist.setId(null);
            whitelist.setCreatedTime(LocalDateTime.now());
            whitelist.setUpdatedTime(LocalDateTime.now());
            // 确保enabled字段有默认值
            if (whitelist.getEnabled() == null) {
                whitelist.setEnabled(true);
            }
            
            boolean success = ruleManager.addWhitelist(whitelist);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("白名单添加成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("白名单添加失败", "保存白名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("添加白名单失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("白名单添加失败", e.getMessage()));
        }
    }
    
    /**
     * 更新白名单
     */
    @PutMapping("/whitelist/{id}")
    public ResponseEntity<Map<String, Object>> updateWhitelist(@PathVariable Long id, @RequestBody FirewallWhitelist whitelist) {
        try {
            whitelist.setId(id);
            whitelist.setUpdatedTime(LocalDateTime.now());
            
            boolean success = ruleManager.updateWhitelist(whitelist);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("白名单更新成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("白名单更新失败", "更新白名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("更新白名单失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("白名单更新失败", e.getMessage()));
        }
    }
    
    /**
     * 删除白名单
     */
    @DeleteMapping("/whitelist/{id}")
    public ResponseEntity<Map<String, Object>> deleteWhitelist(@PathVariable Long id) {
        try {
            boolean success = ruleManager.deleteWhitelist(id);
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("白名单删除成功"));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("白名单删除失败", "删除白名单时发生错误"));
            }
        } catch (Exception e) {
            log.error("删除白名单失败: {}", id, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("白名单删除失败", e.getMessage()));
        }
    }
    
    // ==================== 访问日志查询 ====================
    
    /**
     * 获取访问日志
     */
    @GetMapping("/logs")
    public ResponseEntity<List<FirewallAccessLog>> getAccessLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String apiPath,
            @RequestParam(defaultValue = "100") Integer limit) {
        try {
            // 默认查询最近24小时
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            List<FirewallAccessLog> logs = firewallService.getAccessLogs(startTime, endTime, ipAddress, apiPath, limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("获取访问日志失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<FirewallStatistics>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String apiPath) {
        try {
            // 默认查询最近7天
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(6);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            List<FirewallStatistics> statistics = firewallService.getStatistics(startDate, endDate, apiPath);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrendData(@RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> trendData = firewallService.getTrendData(days);
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            log.error("获取趋势数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取访问最频繁的IP地址
     */
    @GetMapping("/top-ips")
    public ResponseEntity<List<Map<String, Object>>> getTopIpAddresses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            List<Map<String, Object>> topIps = firewallService.getTopIpAddresses(startTime, endTime, limit);
            return ResponseEntity.ok(topIps);
        } catch (Exception e) {
            log.error("获取访问最频繁IP失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取访问最频繁的API路径
     */
    @GetMapping("/top-apis")
    public ResponseEntity<List<Map<String, Object>>> getTopApiPaths(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(24);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            List<Map<String, Object>> topApis = firewallService.getTopApiPaths(startTime, endTime, limit);
            return ResponseEntity.ok(topApis);
        } catch (Exception e) {
            log.error("获取访问最频繁API失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 系统管理 ====================
    
    /**
     * 刷新缓存
     */
    @PostMapping("/refresh-cache")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        try {
            ruleManager.refreshRules();
            ruleManager.refreshBlacklist();
            ruleManager.refreshWhitelist();
            firewallInterceptor.clearCache();
            
            return ResponseEntity.ok(createSuccessResponse("缓存刷新成功"));
        } catch (Exception e) {
            log.error("刷新缓存失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("缓存刷新失败", e.getMessage()));
        }
    }
    
    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Map<String, Object> health = firewallService.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取系统健康状态失败", e.getMessage()));
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message, String detail) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("detail", detail);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}