package com.example.functions;

import com.example.model.ExecutionContext;
import com.example.model.ServerlessFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务示例函数
 */
public class UserServiceFunction implements ServerlessFunction {
    
    // 模拟用户存储
    private static final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(1);
    
    static {
        // 初始化一些测试数据
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1L);
        user1.put("name", "John Doe");
        user1.put("email", "john@example.com");
        users.put(1L, user1);
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2L);
        user2.put("name", "Jane Smith");
        user2.put("email", "jane@example.com");
        users.put(2L, user2);

        idGenerator.set(3);
    }
    
    @Override
    public Object handle(Map<String, Object> input, ExecutionContext context) throws Exception {
        String action = (String) ((Map)input.get("body")).get("action");
        if (action == null) {
            action = "list";
        }
        
        Map<String, Object> result = new HashMap<>();
        
        switch (action.toLowerCase()) {
            case "list":
                result.put("users", users.values());
                result.put("count", users.size());
                break;
                
            case "get":
                Long userId = Long.valueOf(input.get("userId").toString());
                Map<String, Object> user = users.get(userId);
                if (user != null) {
                    result.put("user", user);
                } else {
                    result.put("error", "User not found");
                }
                break;
                
            case "create":
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) ((Map)input.get("body")).get("user");
                Long newId = idGenerator.getAndIncrement();
                userData.put("id", newId);
                users.put(newId, userData);
                result.put("user", userData);
                result.put("message", "User created successfully");
                break;
                
            case "delete":
                Long deleteId = Long.valueOf(input.get("userId").toString());
                Map<String, Object> deletedUser = users.remove(deleteId);
                if (deletedUser != null) {
                    result.put("message", "User deleted successfully");
                } else {
                    result.put("error", "User not found");
                }
                break;
                
            default:
                result.put("error", "Unknown action: " + action);
        }
        
        result.put("action", action);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
}