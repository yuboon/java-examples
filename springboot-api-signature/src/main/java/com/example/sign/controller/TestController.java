package com.example.sign.controller;

import com.example.sign.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 *
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * 需要签名的接口 - GET请求
     */
    @GetMapping("/protected/data")
    public Map<String, Object> getProtectedData(@RequestParam String userId,
                                               @RequestParam String type) {
        log.info("Received protected data request for userId: {}, type: {}", userId, type);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", "This is protected data for user: " + userId);
        result.put("type", type);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 需要签名的接口 - POST请求
     */
    @PostMapping("/protected/create")
    public Map<String, Object> createProtectedData(@RequestBody Map<String, Object> requestData) {
        log.info("Received protected create request: {}", requestData);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Data created successfully");
        result.put("data", requestData);
        result.put("id", "DATA_" + System.currentTimeMillis());
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 需要签名的接口 - 混合参数请求
     */
    @PostMapping("/protected/mixed")
    public Map<String, Object> mixedRequest(@RequestParam String action,
                                           @RequestBody Map<String, Object> requestData) {
        log.info("Received mixed request - action: {}, data: {}", action, requestData);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Mixed request processed successfully");
        result.put("action", action);
        result.put("requestData", requestData);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 公开接口（不需要签名）- 健康检查
     */
    @GetMapping("/public/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Service is healthy");
        result.put("timestamp", System.currentTimeMillis());
        result.put("version", "1.0.0");

        return result;
    }

    /**
     * 公开接口（不需要签名）- 获取服务器信息
     */
    @GetMapping("/public/info")
    public Map<String, Object> getPublicInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "This is public API");
        result.put("data", "Public information");
        result.put("algorithm", "HMAC-SHA256");
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 公开接口（不需要签名）- 生成签名的工具接口（仅用于测试）
     */
    @PostMapping("/public/generate-signature")
    public Map<String, Object> generateSignature(@RequestParam Map<String, Object> params,
                                                @RequestParam String apiKey,
                                                @RequestParam String secret) {
        log.info("Generate signature request for apiKey: {}", apiKey);

        String timestamp = SignatureUtil.getCurrentTimestamp();
        String signature = SignatureUtil.generateSignature(params, timestamp, secret);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Signature generated successfully");
        result.put("timestamp", timestamp);
        result.put("signature", signature);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", apiKey);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Signature", signature);
        result.put("headers", headers);

        return result;
    }

    /**
     * 需要签名的接口 - 获取用户信息
     */
    @GetMapping("/protected/user/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable String userId,
                                           @RequestParam(required = false) String includeDetails) {
        log.info("Get user info request for userId: {}, includeDetails: {}", userId, includeDetails);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", "user_" + userId);
        userInfo.put("email", userId + "@example.com");
        userInfo.put("status", "active");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "User info retrieved successfully");
        result.put("user", userInfo);

        if ("true".equals(includeDetails)) {
            Map<String, Object> details = new HashMap<>();
            details.put("createdTime", "2024-01-01T00:00:00Z");
            details.put("lastLoginTime", "2024-01-15T10:30:00Z");
            details.put("loginCount", 42);
            result.put("details", details);
        }

        result.put("timestamp", System.currentTimeMillis());

        return result;
    }
}