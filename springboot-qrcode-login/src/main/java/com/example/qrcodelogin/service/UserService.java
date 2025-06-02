package com.example.qrcodelogin.service;

import com.example.qrcodelogin.model.UserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {
    
    // 模拟用户数据库
    private static final Map<String, UserInfo> USER_DB = new HashMap<>();
    
    static {
        // 添加一些测试用户
        USER_DB.put("user1", new UserInfo(
                "user1",
                "张三",
                "https://api.dicebear.com/7.x/avataaars/svg?seed=user1",
                "zhangsan@example.com",
                null
        ));
        
        USER_DB.put("user2", new UserInfo(
                "user2",
                "李四",
                "https://api.dicebear.com/7.x/avataaars/svg?seed=user2",
                "lisi@example.com",
                null
        ));
    }
    
    /**
     * 获取所有用户
     */
    public Map<String, UserInfo> getAllUsers() {
        return USER_DB;
    }
    
    /**
     * 模拟登录
     */
    public UserInfo login(String userId) {
        UserInfo userInfo = USER_DB.get(userId);
        if (userInfo != null) {
            // 生成一个新的token
            String token = UUID.randomUUID().toString();
            userInfo.setToken(token);
            return userInfo;
        }
        return null;
    }
    
    /**
     * 验证token
     */
    public UserInfo validateToken(String token) {
        // 简单模拟，实际应用中应该有更复杂的token验证逻辑
        for (UserInfo user : USER_DB.values()) {
            if (token != null && token.equals(user.getToken())) {
                return user;
            }
        }
        return null;
    }
}