package com.example.hotpatch.patches;

import com.example.hotpatch.annotation.HotPatch;
import com.example.hotpatch.annotation.PatchType;
import com.example.hotpatch.example.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * UserService的补丁类 - 修复空指针异常
 */
@HotPatch(
    type = PatchType.SPRING_BEAN,
    originalBean = "userService",
    version = "1.0.1",
    description = "修复getUserInfo空指针异常"
)
@Service
public class UserServicePatch extends UserService {

    @Autowired
    private Environment environment;

    public String getUserInfo(Long userId) {
        System.err.println("environment patch: " + environment.getProperty("server.port"));

        // 修复空指针异常问题
        if (userId == null) {
            return "未知用户"; // 返回默认值而不是null
        }
        
        // 模拟从数据库获取用户信息
        if (userId == 1L) {
            return "Alice";
        } else if (userId == 2L) {
            return "Bob";
        } else {
            return "未知用户"; // 返回默认值而不是null
        }
    }
    
    public int getUserNameLength(Long userId) {
        String userName = getUserInfo(userId);
        return userName != null ? userName.length() : 0; // 安全的长度计算
    }
}