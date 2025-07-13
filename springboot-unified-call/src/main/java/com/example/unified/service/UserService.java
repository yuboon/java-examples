package com.example.unified.service;


import com.example.unified.dto.UserDTO;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public UserDTO getUser(Long id) {
        // 模拟数据库查询
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUsername("zhangsan");
        user.setEmail("zhangsan@example.com");
        user.setAge(28);
        user.setStatus("ACTIVE");
        return user;
    }
}