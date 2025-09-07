package com.example.onlinedebug.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演示控制器 - 用于测试调试功能
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    @Autowired
    private DemoService demoService;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<DemoService.User> getUser(@PathVariable("id") Long id) {
        try {
            DemoService.User user = demoService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<DemoService.User>> getAllUsers() {
        try {
            List<DemoService.User> users = demoService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<DemoService.User> createUser(@RequestBody CreateUserRequest request) {
        try {
            DemoService.User user = demoService.createUser(request.getName(), request.getEmail());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            boolean deleted = demoService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建用户请求对象
     */
    public static class CreateUserRequest {
        private String name;
        private String email;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}