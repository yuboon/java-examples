package com.example.unified.controller;

import com.example.unified.ApiResponse;
import com.example.unified.dto.UserDTO;
import com.example.unified.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/getUser")
    public ApiResponse<UserDTO> getUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.getUser(userDTO.getId());
        return ApiResponse.success(user);
    }

}
