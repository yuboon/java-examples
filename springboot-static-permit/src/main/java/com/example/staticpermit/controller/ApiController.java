package com.example.staticpermit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API控制器，用于演示API访问权限控制
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, Object>> publicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个公开的API端点，任何人都可以访问");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/private/user-info")
    public ResponseEntity<Map<String, Object>> privateUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("message", "这是一个受保护的API端点，需要登录才能访问");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}