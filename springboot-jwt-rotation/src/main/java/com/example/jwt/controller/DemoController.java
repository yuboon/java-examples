package com.example.jwt.controller;

import com.example.jwt.model.ApiResponse;
import com.example.jwt.service.DynamicKeyStore;
import com.example.jwt.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 演示控制器
 * 展示JWT密钥轮换的功能和状态
 */
@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private DynamicKeyStore keyStore;

    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * 获取密钥存储统计信息
     */
    @GetMapping("/key-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKeyStats() {
        Map<String, Object> stats = keyStore.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("获取密钥统计信息成功", stats));
    }

    /**
     * 解析Token（不验证签名）
     */
    @PostMapping("/parse-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请提供有效的Token"));
        }

        String token = authHeader.substring(7);

        try {
            Map<String, Object> result = new HashMap<>();

            // 提取Header信息
            String keyId = jwtTokenService.extractKeyId(token);
            String username = jwtTokenService.extractUsername(token);

            result.put("keyId", keyId);
            result.put("username", username);
            result.put("tokenLength", token.length());
            result.put("tokenPreview", token.substring(0, Math.min(50, token.length())) + "...");

            // 尝试验证Token
            try {
                Claims claims = jwtTokenService.validateToken(token);
                result.put("valid", true);
                result.put("subject", claims.getSubject());
                result.put("issuedAt", claims.getIssuedAt());
                result.put("expiresAt", claims.getExpiration());
                result.put("claims", claims);
            } catch (JwtException e) {
                result.put("valid", false);
                result.put("error", e.getMessage());
            }

            return ResponseEntity.ok(ApiResponse.success("Token解析完成", result));

        } catch (Exception e) {
            logger.error("解析Token失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Token解析失败: " + e.getMessage()));
        }
    }

    /**
     * 演示生成测试Token
     */
    @PostMapping("/generate-test-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateTestToken(
            @RequestParam(defaultValue = "demoUser") String username) {

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "DEMO");
            claims.put("purpose", "演示测试");
            claims.put("generatedAt", System.currentTimeMillis());

            String token = jwtTokenService.generateToken(username, claims);

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("username", username);
            result.put("keyId", jwtTokenService.extractKeyId(token));
            result.put("claims", claims);

            return ResponseEntity.ok(ApiResponse.success("测试Token生成成功", result));

        } catch (Exception e) {
            logger.error("生成测试Token失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("生成Token失败: " + e.getMessage()));
        }
    }

    /**
     * 模拟保护资源的API
     */
    @GetMapping("/protected")
    public ResponseEntity<ApiResponse<Map<String, Object>>> protectedResource(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(ApiResponse.error("需要提供有效的Token"));
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtTokenService.validateToken(token);
            String username = claims.getSubject();
            String keyId = jwtTokenService.extractKeyId(token);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "恭喜！您访问了受保护的资源");
            result.put("username", username);
            result.put("keyId", keyId);
            result.put("accessTime", System.currentTimeMillis());
            result.put("serverTime", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(ApiResponse.success("访问成功", result));

        } catch (JwtException e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Token验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("application", "JWT密钥轮换演示系统");
        info.put("version", "1.0.0");
        info.put("serverTime", java.time.LocalDateTime.now().toString());
        info.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + " ms");

        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("free", runtime.freeMemory() / 1024 / 1024 + " MB");
        memory.put("used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB");
        memory.put("max", runtime.maxMemory() / 1024 / 1024 + " MB");
        info.put("memory", memory);

        return ResponseEntity.ok(ApiResponse.success("系统信息获取成功", info));
    }
}