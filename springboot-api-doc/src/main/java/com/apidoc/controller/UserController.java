package com.apidoc.controller;

import com.apidoc.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 示例用户控制器
 * 用于演示API文档工具的功能
 */
@RestController
@RequestMapping("/api/users")
@ApiGroup(name = "用户管理", description = "用户相关的API接口", order = 1)
public class UserController {

    /**
     * 获取用户列表
     */
    @GetMapping
    @ApiOperation(value = "获取用户列表", description = "分页获取用户列表信息")
    @ApiStatus(ApiStatus.Status.STABLE)
    public List<User> getUsers(@ApiParam(name = "page", description = "页码，从1开始", example = "1", defaultValue = "1")
                              @RequestParam(defaultValue = "1") int page,
                              @ApiParam(name = "size", description = "每页记录数，最大100", example = "10", defaultValue = "10")
                              @RequestParam(defaultValue = "10") int size) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            User user = new User();
            user.setId((long) ((page - 1) * size + i));
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i % 50);
            users.add(user);
        }
        return users;
    }

    @GetMapping("/user2")
    public List<User2> getUsers2(
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        List<User2> users = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            User2 user = new User2();
            user.setId((long) ((page - 1) * size + i));
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            users.add(user);
        }
        return users;
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @ApiStatus(ApiStatus.Status.STABLE)
    public User getUserById(@ApiParam(name = "id", description = "用户唯一标识符", example = "1001", required = true)
                           @PathVariable Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setAge(25);
        return user;
    }

    /**
     * 创建用户
     */
    @PostMapping
    @ApiOperation(value = "创建用户", description = "创建新的用户账户")
    @ApiExample(User.class)
    @ApiStatus(ApiStatus.Status.STABLE)
    public User createUser(@ApiParam(name = "user", description = "用户信息，包含用户名、邮箱、年龄等基本信息", required = true)
                          @RequestBody User user) {
        user.setId(System.currentTimeMillis());
        return user;
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "更新用户", description = "更新指定用户的信息")
    @ApiExample(User.class)
    @ApiStatus(ApiStatus.Status.STABLE)
    public User updateUser(@ApiParam(name = "id", description = "要更新的用户ID", example = "1001", required = true)
                          @PathVariable Long id,
                          @ApiParam(name = "user", description = "更新的用户信息", required = true)
                          @RequestBody User user) {
        user.setId(id);
        return user;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除用户", description = "根据ID删除用户")
    @ApiStatus(ApiStatus.Status.STABLE)
    public Map<String, Object> deleteUser(@ApiParam(name = "id", description = "要删除的用户ID", example = "1001", required = true)
                                         @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户删除成功");
        result.put("id", id);
        return result;
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @ApiOperation(value = "搜索用户", description = "根据关键词搜索用户")
    @ApiStatus(ApiStatus.Status.BETA)
    public List<User> searchUsers(@ApiParam(name = "keyword", description = "搜索关键词，支持用户名和邮箱模糊匹配", example = "john", required = true)
                                 @RequestParam String keyword,
                                 @ApiParam(name = "field", description = "搜索字段，可选值：username, email, phone", example = "username", defaultValue = "username")
                                 @RequestParam(defaultValue = "username") String field) {
        // 模拟搜索结果
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setId(1001L);
        user.setUsername("searchUser");
        user.setEmail("search@example.com");
        user.setAge(28);
        users.add(user);
        return users;
    }

    /**
     * 内部测试接口
     */
    @GetMapping("/internal/stats")
    @ApiOperation(value = "获取用户统计", description = "获取用户相关统计信息")
    @ApiEnvironment({"development", "test"})
    @ApiStatus(ApiStatus.Status.DEVELOPMENT)
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 12345);
        stats.put("activeUsers", 9876);
        stats.put("newUsersToday", 42);
        return stats;
    }

    /**
     * 旧版用户接口（已废弃）
     */
    @GetMapping("/legacy")
    @ApiOperation(value = "旧版用户接口", description = "旧版本的用户获取接口，请使用新版本接口")
    @ApiStatus(ApiStatus.Status.DEPRECATED)
    public Map<String, Object> legacyGetUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "此接口已废弃，请使用 GET /api/users 接口");
        result.put("users", new ArrayList<>());
        return result;
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/upload-avatar")
    @ApiOperation(value = "上传用户头像", description = "为指定用户上传头像图片")
    @ApiStatus(ApiStatus.Status.STABLE)
    public Map<String, Object> uploadAvatar(@ApiParam(name = "userId", description = "用户ID", example = "1001", required = true)
                                          @RequestParam Long userId,
                                          @ApiParam(name = "avatar", description = "头像文件，支持jpg、png格式，大小不超过2MB", required = true)
                                          @RequestParam("avatar") MultipartFile avatar) {
        Map<String, Object> result = new HashMap<>();
        if (avatar.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的文件");
            return result;
        }

        // 模拟文件处理
        result.put("success", true);
        result.put("message", "头像上传成功");
        result.put("userId", userId);
        result.put("filename", avatar.getOriginalFilename());
        result.put("size", avatar.getSize());
        result.put("avatarUrl", "/avatars/" + userId + "_" + avatar.getOriginalFilename());
        return result;
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload-documents")
    @ApiOperation(value = "批量上传文档", description = "为用户批量上传相关文档")
    @ApiStatus(ApiStatus.Status.BETA)
    public Map<String, Object> uploadDocuments(@ApiParam(name = "userId", description = "用户ID", example = "1001", required = true)
                                             @RequestParam Long userId,
                                             @ApiParam(name = "documents", description = "文档文件列表，支持pdf、doc、docx格式", required = true)
                                             @RequestParam("documents") List<MultipartFile> documents,
                                             @ApiParam(name = "category", description = "文档分类", example = "身份证明", defaultValue = "其他")
                                             @RequestParam(defaultValue = "其他") String category) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();

        for (MultipartFile doc : documents) {
            if (!doc.isEmpty()) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("filename", doc.getOriginalFilename());
                fileInfo.put("size", doc.getSize());
                fileInfo.put("url", "/documents/" + userId + "_" + doc.getOriginalFilename());
                uploadedFiles.add(fileInfo);
            }
        }

        result.put("success", true);
        result.put("message", "文档上传成功");
        result.put("userId", userId);
        result.put("category", category);
        result.put("uploadedFiles", uploadedFiles);
        result.put("totalCount", uploadedFiles.size());
        return result;
    }

    /**
     * 用户模型类
     */
    public static class User {
        @ApiField(value = "用户唯一标识符", example = "1001", required = true)
        private Long id;

        @ApiField(value = "用户名", example = "admin", required = true)
        private String username;

        @ApiField(value = "邮箱地址", example = "admin@example.com", required = true)
        private String email;

        @ApiField(value = "年龄", example = "25")
        private Integer age;

        @ApiField(value = "电话号码", example = "13800138000")
        private String phone;

        @ApiField(value = "家庭住址", example = "北京市朝阳区")
        private String address;

        @ApiField(value = "角色信息")
        private Role role;

        // 构造函数
        public User() {}

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }

    /**
     * 用户模型类
     */
    public static class User2 {
        private Long id;

        @ApiField(value = "用户信息2")
        private String username;

        private String email;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

    }

    /**
     * 角色模型类
     */
    public static class Role {
        @ApiField(value = "角色唯一标识符", example = "1001", required = true)
        private Long id;

        @ApiField(value = "角色名", example = "admin_role", required = true)
        private String roleName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }
}