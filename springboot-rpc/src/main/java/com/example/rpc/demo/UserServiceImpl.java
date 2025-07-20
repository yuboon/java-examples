package com.example.rpc.demo;

import com.example.rpc.core.RpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RpcService(UserService.class)
public class UserServiceImpl implements UserService {
    
    private static final Map<Long, String> userDatabase = new ConcurrentHashMap<>();
    
    static {
        userDatabase.put(1L, "张三");
        userDatabase.put(2L, "李四");
        userDatabase.put(3L, "王五");
    }
    
    @Override
    public String getUserName(Long userId) {
        String userName = userDatabase.get(userId);
        return userName != null ? userName : "用户不存在";
    }
    
    @Override
    public boolean updateUser(Long userId, String name) {
        if (userDatabase.containsKey(userId)) {
            userDatabase.put(userId, name);
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> getUserList() {
        return new ArrayList<>(userDatabase.values());
    }
}