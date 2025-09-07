package com.example.onlinedebug.demo;

import org.springframework.stereotype.Service;

/**
 * 演示服务类 - 用于测试调试功能
 */
@Service
public class DemoService {
    
    /**
     * 获取用户信息
     */
    public User getUserById(Long id) {
        // 模拟一些业务逻辑
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        if (id <= 0) {
            return null;
        }
        
        // 模拟数据库查询延时
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return new User(id, "User" + id, "user" + id + "@example.com");
    }
    
    /**
     * 获取所有用户
     */
    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            users.add(getUserById(i));
        }
        return users;
    }
    
    /**
     * 创建用户
     */
    public User createUser(String name, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        
        // 模拟生成ID
        long id = System.currentTimeMillis() % 1000;
        
        User user = new User(id, name, email);
        
        // 模拟保存操作
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return user;
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        if (id == null || id <= 0) {
            return false;
        }
        
        // 模拟删除操作
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }
    
    /**
     * 用户实体类
     */
    public static class User {
        private Long id;
        private String name;
        private String email;
        
        public User(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
        }
    }
}