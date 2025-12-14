package com.example.cli.service;

import com.example.cli.CommandHandler;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 角色服务示例
 */
@Service("roleService")
public class RoleService implements CommandHandler {

    private final Map<String, Set<String>> userRoles = new HashMap<>();
    private final Map<String, String> roleDescriptions = new HashMap<>();

    public RoleService() {
        // 初始化角色数据
        roleDescriptions.put("admin", "系统管理员");
        roleDescriptions.put("user", "普通用户");
        roleDescriptions.put("guest", "访客");
        roleDescriptions.put("developer", "开发者");
        roleDescriptions.put("operator", "运维人员");

        // 初始化用户角色关系
        userRoles.put("1", new HashSet<>(Arrays.asList("admin", "developer")));
        userRoles.put("2", new HashSet<>(Arrays.asList("user")));
        userRoles.put("3", new HashSet<>(Arrays.asList("user", "operator")));
        userRoles.put("4", new HashSet<>(Arrays.asList("guest")));
    }

    @Override
    public String handle(String[] args) {
        if (args.length == 0) {
            return getUsage();
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "list":
                return listRoles();
            case "users":
                if (args.length < 2) {
                    return "错误：请提供角色名称\n用法: roleService users <roleName>";
                }
                return getUsersByRole(args[1]);
            case "check":
                if (args.length < 3) {
                    return "错误：请提供用户ID和角色名称\n用法: roleService check <userId> <roleName>";
                }
                return checkUserRole(args[1], args[2]);
            case "info":
                if (args.length < 2) {
                    return listRoles();
                }
                return getRoleInfo(args[1]);
            default:
                return "未知命令: " + command + "\n" + getUsage();
        }
    }

    private String listRoles() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用角色列表:\n");
        sb.append("-".repeat(40)).append("\n");

        roleDescriptions.forEach((role, desc) -> {
            long userCount = userRoles.values().stream()
                    .filter(roles -> roles.contains(role))
                    .count();
            sb.append(String.format("%s - %s (用户数: %d)\n", role, desc, userCount));
        });

        return sb.toString();
    }

    private String getUsersByRole(String roleName) {
        if (!roleDescriptions.containsKey(roleName)) {
            return "角色不存在: " + roleName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("拥有角色 [%s] 的用户:\n", roleName));
        sb.append("-".repeat(40)).append("\n");

        userRoles.entrySet().stream()
                .filter(entry -> entry.getValue().contains(roleName))
                .forEach(entry -> {
                    sb.append(String.format("用户ID: %s\n", entry.getKey()));
                });

        return sb.toString();
    }

    private String checkUserRole(String userId, String roleName) {
        if (!roleDescriptions.containsKey(roleName)) {
            return "角色不存在: " + roleName;
        }

        Set<String> roles = userRoles.get(userId);
        if (roles == null) {
            return "用户不存在: " + userId;
        }

        boolean hasRole = roles.contains(roleName);
        return String.format("用户 %s %s角色 [%s]",
                userId, hasRole ? "拥有" : "没有", roleName);
    }

    private String getRoleInfo(String roleName) {
        if (!roleDescriptions.containsKey(roleName)) {
            return "角色不存在: " + roleName;
        }

        long userCount = userRoles.values().stream()
                .filter(roles -> roles.contains(roleName))
                .count();

        return String.format("""
                角色信息:
                名称: %s
                描述: %s
                用户数: %d
                """, roleName, roleDescriptions.get(roleName), userCount);
    }

    @Override
    public String getDescription() {
        return "角色管理服务";
    }

    @Override
    public String getUsage() {
        return """
                角色服务使用说明:

                命令格式: roleService <command> [args]

                可用命令:
                  list           - 列出所有角色
                  users <role>   - 查看拥有指定角色的用户
                  check <id> <role> - 检查用户是否拥有指定角色
                  info [role]    - 获取角色信息

                示例:
                  roleService list           - 列出所有角色
                  roleService users admin    - 查看管理员用户
                  roleService check 1 admin  - 检查用户1是否是管理员
                  roleService info user      - 获取user角色信息
                """;
    }
}