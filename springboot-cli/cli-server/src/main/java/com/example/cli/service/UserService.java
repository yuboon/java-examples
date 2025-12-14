package com.example.cli.service;

import com.example.cli.CommandHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务示例
 */
@Service("userService")
public class UserService implements CommandHandler {

    private final Map<String, Map<String, String>> users = new HashMap<>();

    public UserService() {
        // 初始化一些示例数据
        Map<String, String> user1 = new HashMap<>();
        user1.put("id", "1");
        user1.put("name", "张三");
        user1.put("email", "zhangsan@example.com");
        user1.put("type", "admin");
        users.put("1", user1);

        Map<String, String> user2 = new HashMap<>();
        user2.put("id", "2");
        user2.put("name", "李四");
        user2.put("email", "lisi@example.com");
        user2.put("type", "user");
        users.put("2", user2);

        Map<String, String> user3 = new HashMap<>();
        user3.put("id", "3");
        user3.put("name", "王五");
        user3.put("email", "wangwu@example.com");
        user3.put("type", "user");
        users.put("3", user3);
    }

    @Override
    public String handle(String[] args) {
        if (args.length == 0) {
            return getUsage();
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "list":
                return listUsers(args.length > 1 ? args[1] : null);
            case "get":
                if (args.length < 2) {
                    return "错误：请提供用户ID\n用法: userService get <userId>";
                }
                return getUser(args[1]);
            case "count":
                return countUsers(args.length > 1 ? args[1] : null);
            default:
                return "未知命令: " + command + "\n" + getUsage();
        }
    }

    private String listUsers(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户列表:\n");
        sb.append("-".repeat(60)).append("\n");

        users.values().stream()
                .filter(user -> type == null || type.equalsIgnoreCase(user.get("type")))
                .forEach(user -> {
                    sb.append(String.format("ID: %s, 姓名: %s, 邮箱: %s, 类型: %s\n",
                            user.get("id"), user.get("name"),
                            user.get("email"), user.get("type")));
                });

        return sb.toString();
    }

    private String getUser(String userId) {
        Map<String, String> user = users.get(userId);
        if (user == null) {
            return "用户不存在: " + userId;
        }

        return String.format("""
                用户详情:
                ID: %s
                姓名: %s
                邮箱: %s
                类型: %s
                """, user.get("id"), user.get("name"),
                user.get("email"), user.get("type"));
    }

    private String countUsers(String type) {
        long count = users.values().stream()
                .filter(user -> type == null || type.equalsIgnoreCase(user.get("type")))
                .count();

        if (type != null) {
            return String.format("%s类型的用户数量: %d", type, count);
        } else {
            return String.format("总用户数量: %d", count);
        }
    }

    @Override
    public String getDescription() {
        return "用户管理服务";
    }

    @Override
    public String getUsage() {
        return """
                用户服务使用说明:

                命令格式: userService <command> [args]

                可用命令:
                  list [type]  - 列出用户，可指定类型(admin/user)
                  get <id>     - 获取指定ID的用户详情
                  count [type] - 统计用户数量，可指定类型

                示例:
                  userService list        - 列出所有用户
                  userService list admin  - 列出管理员用户
                  userService get 1       - 获取ID为1的用户
                  userService count       - 统计总用户数
                  userService count user  - 统计普通用户数
                """;
    }
}