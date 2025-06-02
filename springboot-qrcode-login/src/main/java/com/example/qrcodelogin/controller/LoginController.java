package com.example.qrcodelogin.controller;

import com.example.qrcodelogin.model.UserInfo;
import com.example.qrcodelogin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class LoginController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 验证token并获取用户信息
     */
    @PostMapping("/validate")
    public ResponseEntity<UserInfo> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        UserInfo userInfo = userService.validateToken(token);
        if (userInfo == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        log.info("Token validated for user: {}", userInfo.getUsername());
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 获取可用的测试用户列表 (仅用于演示)
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, UserInfo>> getTestUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}