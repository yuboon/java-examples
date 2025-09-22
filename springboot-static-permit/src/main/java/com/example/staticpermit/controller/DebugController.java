package com.example.staticpermit.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试控制器，用于检查认证状态
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/auth")
    public Map<String, Object> getAuthInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new HashMap<>();

        if (auth != null) {
            result.put("name", auth.getName());
            result.put("authorities", auth.getAuthorities().toString());
            result.put("isAuthenticated", auth.isAuthenticated());
            result.put("principal", auth.getPrincipal().toString());
            result.put("class", auth.getClass().getSimpleName());
        } else {
            result.put("message", "No authentication found");
        }

        return result;
    }
}