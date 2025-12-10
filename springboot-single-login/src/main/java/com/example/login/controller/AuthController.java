package com.example.login.controller;

import com.example.login.config.LoginProperties;
import com.example.login.model.ApiResponse;
import com.example.login.model.LoginInfo;
import com.example.login.model.LoginRequest;
import com.example.login.model.TokenInfo;
import com.example.login.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 认证控制器（API接口）
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private LoginProperties loginProperties;

    // 模拟用户数据库
    private static final Map<String, String> USER_DB = new HashMap<>();
    static {
        USER_DB.put("admin", "admin123");
        USER_DB.put("user1", "user123");
        USER_DB.put("user2", "user123");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. 验证用户名密码
        if (!USER_DB.containsKey(username) ||
            !USER_DB.get(username).equals(password)) {
            return ResponseEntity.status(401)
                .body(ApiResponse.fail("用户名或密码错误"));
        }

        // 2. 创建登录信息
        LoginInfo loginInfo = new LoginInfo(
            getClientIp(httpRequest),
            getClientDevice(httpRequest),
            httpRequest.getHeader("User-Agent"),
            System.currentTimeMillis()
        );

        // 3. 执行登录
        String token = sessionManager.login(username, loginInfo);

        // 4. 返回登录结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("expireTime", System.currentTimeMillis() +
                loginProperties.getTokenExpireTime() * 1000);
        data.put("loginMode", loginProperties.getMode());

        return ResponseEntity.ok(ApiResponse.success("登录成功", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "${app.login.token-header:Authorization}", required = false) String token) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(token)) {
            sessionManager.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success("退出登录成功"));
    }

    @PostMapping("/kickout")
    public ResponseEntity<?> kickout(@RequestParam String username) {
        sessionManager.kickoutUser(username);
        return ResponseEntity.ok(ApiResponse.success("已踢出用户：" + username));
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers() {
        Set<String> users = sessionManager.getOnlineUsers();
        return ResponseEntity.ok(ApiResponse.success("获取成功", users));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        TokenInfo tokenInfo = (TokenInfo) request.getAttribute("tokenInfo");
        if (tokenInfo == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.fail(401, "未登录"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", tokenInfo.getUsername());
        data.put("loginTime", tokenInfo.getLoginInfo().getLoginTime());
        data.put("ip", tokenInfo.getLoginInfo().getIp());
        data.put("device", tokenInfo.getLoginInfo().getDevice());
        data.put("userAgent", tokenInfo.getLoginInfo().getUserAgent());
        data.put("expireTime", tokenInfo.getExpireTime());

        return ResponseEntity.ok(ApiResponse.success("获取成功", data));
    }

    @GetMapping("/tokens")
    public ResponseEntity<?> getUserTokens(@RequestParam String username) {
        List<String> tokens = sessionManager.getUserTokens(username);
        return ResponseEntity.ok(ApiResponse.success("获取成功", tokens));
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取客户端设备信息
     */
    private String getClientDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "PC";
        }
    }
}