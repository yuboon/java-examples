package com.example.livestream.service;

import com.example.livestream.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserService {

    // 内存中存储用户信息
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserService() {
        // 初始化一些测试用户
        users.put("zb1", new User("zb1", "zb1", "主播一号",
                "avatar1.jpg", User.UserRole.BROADCASTER));
        users.put("zb2", new User("zb2", "zb2", "主播二号",
                "avatar2.jpg", User.UserRole.BROADCASTER));
        users.put("gz1", new User("gz1", "gz1", "观众一号",
                "user1.jpg", User.UserRole.AUDIENCE));
        users.put("gz2", new User("gz2", "gz2", "观众二号",
                "user2.jpg", User.UserRole.AUDIENCE));
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public boolean validateUser(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public User registerUser(User user) {
        if (users.containsKey(user.getUsername())) {
            return null;
        }
        users.put(user.getUsername(), user);
        return user;
    }
}