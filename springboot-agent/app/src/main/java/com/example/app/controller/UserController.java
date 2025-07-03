package com.example.app.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/user")
@RestController
public class UserController {

    // 用户查询
    @RequestMapping("/query")
    public List<String> queryUser() {
        // 模拟查询用户
        return List.of("user1", "user2", "user3");
    }

}
