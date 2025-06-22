package com.example.face2face.controller;

import com.example.face2face.dto.request.LoginRequest;
import com.example.face2face.dto.request.RegisterRequest;
import com.example.face2face.dto.response.ApiResponse;
import com.example.face2face.dto.response.JwtResponse;
import com.example.face2face.model.User;
import com.example.face2face.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ApiResponse.success(user);
    }

    @PostMapping("/login")
    public ApiResponse<JwtResponse> login(@RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
}