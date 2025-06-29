package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 目标服务接口
 */
@RestController
@RequestMapping("/user")
@ConditionalOnProperty(name = "user.enabled", havingValue = "true", matchIfMissing = false)
public class UserController {

    @Autowired
    private Environment environment;

    @GetMapping("/users1")
    public Map<String, String> getUser1() {
        Map<String, String> result = new HashMap<>();
        result.put("user", "AAA");
        result.put("port", environment.getProperty("server.port"));
        return result;
    }

    @PostMapping("/add")
    public Map<String, String> addUser(@RequestBody Map<String, String> data) {
        Map<String, String> result = new HashMap<>();
        result.put("msg", data.get("name") + " add ok");
        result.put("port", environment.getProperty("server.port"));
        return result;
    }

    @GetMapping("/users2")
    public Map<String, String> getUser2() {
        Map<String, String> result = new HashMap<>();
        result.put("user", "BBB");
        result.put("port", environment.getProperty("server.port"));
        return result;
    }

}