package com.example.rpc.demo;

import java.util.List;

public interface UserService {
    String getUserName(Long userId);
    boolean updateUser(Long userId, String name);
    List<String> getUserList();
}