package com.example.hotpatch.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 示例用户服务 - 用于演示热补丁功能
 */
@Service
public class UserService {

    @Autowired
    private Environment environment;

    public String getUserInfo(Long userId) {
        System.err.println("environment: " + environment.getProperty("server.port"));
        // 故意包含一个空指针异常的bug
        if (userId == null) {
            return null; // 这里会导致后续调用出现问题
        }
        
        // 模拟从数据库获取用户信息
        if (userId == 1L) {
            return "Alice";
        } else if (userId == 2L) {
            return "Bob";
        } else {
            return null; // 这里会导致后续调用 .length() 时出现空指针异常
        }
    }
    
    public int getUserNameLength(Long userId) {
        String userName = getUserInfo(userId);
        return userName.length(); // 当userName为null时会抛出空指针异常
    }
}