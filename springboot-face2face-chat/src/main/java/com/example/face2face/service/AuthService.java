package com.example.face2face.service;

import com.example.face2face.dto.request.LoginRequest;
import com.example.face2face.dto.request.RegisterRequest;
import com.example.face2face.dto.response.JwtResponse;
import com.example.face2face.exception.AppException;
import com.example.face2face.model.User;
import com.example.face2face.service.UserService;
import com.example.face2face.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    
    public User register(RegisterRequest request) {
        return userService.registerUser(request);
    }

    
    public JwtResponse login(LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername());
        if (user == null) {
            throw new AppException(401, "Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(401, "Invalid username or password");
        }

        String token = jwtTokenUtil.generateToken(user.getUsername(), user.getId());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }
}