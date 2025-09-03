package com.example.hotpatch.controller;

import com.example.hotpatch.example.MathHelper;
import com.example.hotpatch.example.StringUtils;
import com.example.hotpatch.example.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 测试控制器 - 用于测试热补丁功能
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private UserService userService;
    
    // 测试Spring Bean补丁
    @GetMapping("/user")
    public String testUser(@RequestParam(value = "id",required = false) Long id) {
        try {
            int userNameLength = userService.getUserNameLength(id);
            return "用户名长度: " + userNameLength;
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }
    
    // 测试工具类补丁
    @GetMapping("/string-utils")
    public boolean testStringUtils(@RequestParam(defaultValue = "  ") String str) {
        return StringUtils.isEmpty(str);
    }
    
    // 测试静态方法补丁
    @GetMapping("/math/{a}/{b}")
    public String testMath(@PathVariable int a, @PathVariable int b) {
        try {
            int result = MathHelper.divide(a, b);
            return "计算结果: " + a + " / " + b + " = " + result;
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }
}