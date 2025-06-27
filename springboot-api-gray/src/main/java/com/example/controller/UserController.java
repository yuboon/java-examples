package com.example.controller;

import com.example.model.User;
import com.example.version.ApiVersion;
import com.example.version.GrayRelease;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 模拟数据库
    private final List<User> userDatabase = new ArrayList<>();
    
    public UserController() {
        // 初始化一些测试数据
        userDatabase.add(new User(
            1L, "john_doe", "John Doe", "john@example.com",
            LocalDateTime.now().minusDays(100), LocalDateTime.now().minusDays(10),
            true,"1234567890","https://example.com/avatars/john.jpg")
        );

        userDatabase.add(new User(
                2L, "john_doe2", "John Doe2", "john2@example.com",
                LocalDateTime.now().minusDays(102), LocalDateTime.now().minusDays(12),
                true,"9876543210","https://example.com/avatars/john.jpg")
        );
    }
    
    /**
     * 获取用户列表 - 版本1.0
     * 只返回基本用户信息
     */
    @GetMapping
    @ApiVersion("1.0")
    public List<User> getUsersV1() {
        return userDatabase.stream()
            .map(user -> {
                User simpleUser = new User();
                simpleUser.setId(user.getId());
                simpleUser.setUsername(user.getUsername());
                simpleUser.setName(user.getName());
                simpleUser.setEmail(user.getEmail());
                simpleUser.setActive(user.getActive());
                return simpleUser;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 获取用户列表 - 版本2.0（包含更多用户信息）
     * 返回完整的用户信息
     */
    @GetMapping
    @ApiVersion("2.0")
    @GrayRelease(
        startTime = "2023-01-01 00:00:00",
        //endTime = "2023-12-31 23:59:59",
        endTime = "2025-12-31 23:59:59",
        userGroups = {"vip", "beta-tester"},
        percentage = 20
    )
    public List<User> getUsersV2() {
        return userDatabase;
    }
    
    /**
     * 获取单个用户 - 版本1.0
     * 只返回基本用户信息
     */
    @GetMapping("/{id}")
    @ApiVersion("1.0")
    public User getUserV1(@PathVariable Long id) {
        User user = findUserById(id);
        if (user == null) {
            return null;
        }
        
        User simpleUser = new User();
        simpleUser.setId(user.getId());
        simpleUser.setUsername(user.getUsername());
        simpleUser.setName(user.getName());
        simpleUser.setEmail(user.getEmail());
        simpleUser.setActive(user.getActive());
        return simpleUser;
    }
    
    /**
     * 获取单个用户 - 版本2.0（包含更多用户信息）
     * 返回完整的用户信息
     */
    @GetMapping("/{id}")
    @ApiVersion("2.0")
    @GrayRelease(
        userIds = "100,101,102",
        regions = {"CN-BJ", "CN-SH"}
    )
    public User getUserV2(@PathVariable Long id) {
        return findUserById(id);
    }
    
    /**
     * 创建用户 - 版本1.0
     * 只需要基本用户信息
     */
    @PostMapping
    @ApiVersion("1.0")
    public User createUserV1(@RequestBody User user) {
        // 设置ID和时间戳
        user.setId((long) (userDatabase.size() + 1));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        
        // 存储用户（实际项目中会保存到数据库）
        userDatabase.add(user);
        
        // 返回简化版本的用户信息
        User simpleUser = new User();
        simpleUser.setId(user.getId());
        simpleUser.setUsername(user.getUsername());
        simpleUser.setName(user.getName());
        simpleUser.setEmail(user.getEmail());
        simpleUser.setPhone(user.getPhone());
        simpleUser.setActive(user.getActive());
        return simpleUser;
    }
    
    /**
     * 创建用户 - 版本2.0（增加了参数验证和更丰富的返回信息）
     */
    @PostMapping
    @ApiVersion("2.0")
    @GrayRelease(percentage = 50)
    public User createUserV2(@RequestBody User user) {
        // 参数验证
        if (user.getName() == null || user.getName().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // 设置ID和时间戳
        user.setId((long) (userDatabase.size() + 1));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // 存储用户
        userDatabase.add(user);
        
        // 返回完整的用户信息
        return user;
    }
    
    /**
     * 更新用户 - 版本1.0
     */
    @PutMapping("/{id}")
    @ApiVersion("1.0")
    public User updateUserV1(@PathVariable Long id, @RequestBody User userUpdate) {
        User existingUser = findUserById(id);
        if (existingUser == null) {
            return null;
        }
        
        // 更新基本字段
        if (userUpdate.getUsername() != null) existingUser.setUsername(userUpdate.getUsername());
        if (userUpdate.getName() != null) existingUser.setName(userUpdate.getName());
        if (userUpdate.getEmail() != null) existingUser.setEmail(userUpdate.getEmail());
        if (userUpdate.getActive() != null) existingUser.setActive(userUpdate.getActive());
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        // 返回简化版本
        User simpleUser = new User();
        simpleUser.setId(existingUser.getId());
        simpleUser.setUsername(existingUser.getUsername());
        simpleUser.setName(existingUser.getName());
        simpleUser.setEmail(existingUser.getEmail());
        simpleUser.setPhone(existingUser.getPhone());
        simpleUser.setActive(existingUser.getActive());
        return simpleUser;
    }
    
    /**
     * 更新用户 - 版本2.0（支持更新更多字段）
     */
    @PutMapping("/{id}")
    @ApiVersion("2.0")
    @GrayRelease(
        userGroups = {"vip", "admin"},
        percentage = 30
    )
    public User updateUserV2(@PathVariable Long id, @RequestBody User userUpdate) {
        User existingUser = findUserById(id);
        if (existingUser == null) {
            return null;
        }
        
        // 更新基本字段
        if (userUpdate.getUsername() != null) existingUser.setUsername(userUpdate.getUsername());
        if (userUpdate.getName() != null) existingUser.setName(userUpdate.getName());
        if (userUpdate.getEmail() != null) existingUser.setEmail(userUpdate.getEmail());
        if (userUpdate.getActive() != null) existingUser.setActive(userUpdate.getActive());

        // 更新扩展字段
        if (userUpdate.getPhone() != null) existingUser.setPhone(userUpdate.getPhone());
        if (userUpdate.getAvatar() != null) existingUser.setAvatar(userUpdate.getAvatar());

        existingUser.setUpdatedAt(LocalDateTime.now());
        
        // 返回完整的用户信息
        return existingUser;
    }
    
    /**
     * 删除用户 - 版本1.0
     */
    @DeleteMapping("/{id}")
    @ApiVersion("1.0")
    public void deleteUserV1(@PathVariable Long id) {
        userDatabase.removeIf(user -> user.getId().equals(id));
    }
    
    /**
     * 删除用户 - 版本2.0（带有软删除功能）
     */
    @DeleteMapping("/{id}")
    @ApiVersion("2.0")
    @GrayRelease(
        userGroups = {"admin"},
        percentage = 10
    )
    public User deleteUserV2(@PathVariable Long id) {
        User existingUser = findUserById(id);
        if (existingUser == null) {
            return null;
        }
        
        // 软删除，而不是物理删除
        existingUser.setActive(false);
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        return existingUser;
    }
    
    /**
     * 查找用户的辅助方法
     */
    private User findUserById(Long id) {
        return userDatabase.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}