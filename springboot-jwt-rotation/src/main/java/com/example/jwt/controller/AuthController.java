package com.example.jwt.controller;

import com.example.jwt.model.ApiResponse;
import com.example.jwt.service.JwtTokenService;
import com.example.jwt.service.KeyRotationScheduler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 提供登录、Token验证、刷新等功能
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private KeyRotationScheduler keyRotationScheduler;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        logger.info("用户登录请求: {}", request.getUsername());

        // 简化的用户验证（实际项目中应该查询数据库）
        if (!isValidUser(request.getUsername(), request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("用户名或密码错误"));
        }

        try {
            // 生成Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "USER");
            claims.put("loginTime", System.currentTimeMillis());

            String token = jwtTokenService.generateToken(request.getUsername(), claims);

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", request.getUsername());
            response.put("expiresIn", 24 * 60 * 60); // 24小时，单位秒
            response.put("tokenType", "Bearer");

            logger.info("用户 {} 登录成功", request.getUsername());

            return ResponseEntity.ok(ApiResponse.success("登录成功", response));

        } catch (Exception e) {
            logger.error("生成Token失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("系统错误，请稍后重试"));
        }
    }

    /**
     * 验证Token
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("缺少有效的Authorization头"));
        }

        String token = authHeader.substring(7); // 移除 "Bearer " 前缀

        try {
            Claims claims = jwtTokenService.validateToken(token);

            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", claims.getSubject());
            userInfo.put("role", claims.get("role"));
            userInfo.put("loginTime", claims.get("loginTime"));
            userInfo.put("issuedAt", claims.getIssuedAt());
            userInfo.put("expiresAt", claims.getExpiration());
            userInfo.put("keyId", jwtTokenService.extractKeyId(token));

            return ResponseEntity.ok(ApiResponse.success("Token有效", userInfo));

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token无效或已过期: " + e.getMessage()));
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("缺少有效的Authorization头"));
        }

        String token = authHeader.substring(7);

        try {
            String newToken = jwtTokenService.refreshToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("expiresIn", 24 * 60 * 60);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(ApiResponse.success("Token刷新成功", response));

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token刷新失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("缺少有效的Authorization头"));
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtTokenService.validateToken(token);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", claims.getSubject());
            userInfo.put("role", claims.get("role"));
            userInfo.put("loginTime", claims.get("loginTime"));

            return ResponseEntity.ok(ApiResponse.success(userInfo));

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token无效: " + e.getMessage()));
        }
    }

    /**
     * 手动触发密钥轮换（管理员功能）
     */
    @PostMapping("/admin/rotate-keys")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotateKeys() {
        try {
            String newKeyId = keyRotationScheduler.forceKeyRotation();

            Map<String, Object> result = new HashMap<>();
            result.put("newKeyId", newKeyId);
            result.put("message", "密钥轮换成功");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success("密钥轮换成功", result));
        } catch (Exception e) {
            logger.error("手动密钥轮换失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("密钥轮换失败: " + e.getMessage()));
        }
    }

    /**
     * 手动清理过期密钥（管理员功能）
     */
    @PostMapping("/admin/cleanup-keys")
    public ResponseEntity<ApiResponse<List<String>>> cleanupKeys() {
        try {
            List<String> removedKeys = keyRotationScheduler.forceKeyCleanup();
            return ResponseEntity.ok(ApiResponse.success("密钥清理完成", removedKeys));
        } catch (Exception e) {
            logger.error("手动密钥清理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("密钥清理失败: " + e.getMessage()));
        }
    }

    /**
     * 简化的用户验证逻辑
     * 实际项目中应该查询数据库进行验证
     */
    private boolean isValidUser(String username, String password) {
        // 简化的验证逻辑，仅用于演示
        // 实际项目中应该使用数据库查询和密码加密验证
        return "admin".equals(username) && "password".equals(password) ||
               "user".equals(username) && "123456".equals(password) ||
               "test".equals(username) && "test".equals(password);
    }

    /**
     * 登录请求对象
     */
    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}