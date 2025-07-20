package com.example.rpc.demo;

import com.example.rpc.core.RpcReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    
    @RpcReference
    private UserService userService;
    
    @GetMapping("/{userId}")
    public String getUser(@PathVariable Long userId) {
        return userService.getUserName(userId);
    }
    
    @PostMapping("/{userId}")
    public boolean updateUser(@PathVariable Long userId, @RequestParam String name) {
        return userService.updateUser(userId, name);
    }
    
    @GetMapping("/list")
    public List<String> getUserList() {
        return userService.getUserList();
    }
}