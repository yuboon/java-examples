package com.example.firewall.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 测试控制器 - 用于测试防火墙功能
 * 提供各种类型的API接口用于验证防火墙的拦截和放行功能
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 简单的GET请求测试
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Test API!");
        response.put("timestamp", LocalDateTime.now());
        response.put("method", "GET");
        return ResponseEntity.ok(response);
    }

    /**
     * 带参数的GET请求测试
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id, 
                                                       @RequestParam(required = false) String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("name", name != null ? name : "User " + id);
        response.put("email", "user" + id + "@example.com");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * POST请求测试
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", new Random().nextInt(1000) + 1);
        response.put("name", userData.get("name"));
        response.put("email", userData.get("email"));
        response.put("created", LocalDateTime.now());
        response.put("status", "created");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT请求测试
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, 
                                                          @RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("name", userData.get("name"));
        response.put("email", userData.get("email"));
        response.put("updated", LocalDateTime.now());
        response.put("status", "updated");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE请求测试
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("deleted", LocalDateTime.now());
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟高频请求的接口 - 用于测试QPS限制
     */
    @GetMapping("/high-frequency")
    public ResponseEntity<Map<String, Object>> highFrequency() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "High frequency API call");
        response.put("timestamp", LocalDateTime.now());
        response.put("requestId", UUID.randomUUID().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟敏感操作的接口 - 用于测试规则匹配
     */
    @PostMapping("/sensitive")
    public ResponseEntity<Map<String, Object>> sensitiveOperation(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensitive operation executed");
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now());
        response.put("warning", "This is a sensitive operation");
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟管理员接口 - 用于测试权限控制
     */
    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin dashboard data");
        response.put("users", 1250);
        response.put("orders", 3456);
        response.put("revenue", 125000.50);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟文件上传接口
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") String fileName,
                                                          @RequestParam(required = false) String description) {
        Map<String, Object> response = new HashMap<>();
        response.put("fileName", fileName);
        response.put("description", description);
        response.put("size", new Random().nextInt(1000000) + 1000);
        response.put("uploadTime", LocalDateTime.now());
        response.put("status", "uploaded");
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟数据导出接口 - 可能被恶意利用
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportData(@RequestParam(required = false) String format,
                                                          @RequestParam(required = false) String table) {
        Map<String, Object> response = new HashMap<>();
        response.put("format", format != null ? format : "csv");
        response.put("table", table != null ? table : "users");
        response.put("records", new Random().nextInt(10000) + 100);
        response.put("exportTime", LocalDateTime.now());
        response.put("downloadUrl", "/downloads/export_" + UUID.randomUUID() + "." + (format != null ? format : "csv"));
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟搜索接口 - 可能被用于SQL注入测试
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String query,
                                                      @RequestParam(defaultValue = "10") int limit,
                                                      @RequestParam(defaultValue = "0") int offset) {
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("limit", limit);
        response.put("offset", offset);
        response.put("total", new Random().nextInt(1000));
        response.put("results", generateSearchResults(query, limit));
        response.put("searchTime", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟登录接口
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> credentials) {
        String username = (String) credentials.get("username");
        String password = (String) credentials.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟简单的登录验证
        if ("admin".equals(username) && "password".equals(password)) {
            response.put("success", true);
            response.put("token", "jwt_token_" + UUID.randomUUID());
            response.put("user", Map.of(
                "id", 1,
                "username", username,
                "role", "admin"
            ));
        } else {
            response.put("success", false);
            response.put("error", "Invalid credentials");
        }
        
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟注册接口
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", new Random().nextInt(10000) + 1000);
        response.put("username", userData.get("username"));
        response.put("email", userData.get("email"));
        response.put("registered", LocalDateTime.now());
        response.put("status", "registered");
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟密码重置接口
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("email", data.get("email"));
        response.put("resetToken", "reset_" + UUID.randomUUID());
        response.put("expiresAt", LocalDateTime.now().plusHours(1));
        response.put("message", "Password reset email sent");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        response.put("uptime", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 模拟慢查询接口 - 用于测试响应时间
     */
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> slowQuery(@RequestParam(defaultValue = "1000") int delay) {
        try {
            Thread.sleep(Math.min(delay, 5000)); // 最多延迟5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Slow query completed");
        response.put("delay", delay);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 生成搜索结果的辅助方法
     */
    private List<Map<String, Object>> generateSearchResults(String query, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 10); i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", i + 1);
            result.put("title", "Result " + (i + 1) + " for: " + query);
            result.put("description", "This is a sample search result description");
            result.put("score", Math.random());
            results.add(result);
        }
        return results;
    }

    /**
     * 获取当前请求信息 - 用于调试
     */
    @GetMapping("/request-info")
    public ResponseEntity<Map<String, Object>> getRequestInfo(
            @RequestHeader Map<String, String> headers,
            @RequestParam Map<String, String> params) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("headers", headers);
        response.put("params", params);
        response.put("timestamp", LocalDateTime.now());
        response.put("method", "GET");
        response.put("path", "/api/test/request-info");
        
        return ResponseEntity.ok(response);
    }
}