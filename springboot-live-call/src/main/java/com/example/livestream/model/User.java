package com.example.livestream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private UserRole role; // BROADCASTER or AUDIENCE
    
    public enum UserRole {
        BROADCASTER,
        AUDIENCE
    }
}