package com.example.face2face.dto.response;

public class JwtResponse {
    private String token;
    private String id;
    private String username;
    private String nickname;

    public JwtResponse() {
    }

    public JwtResponse(String token, String id, String username, String nickname) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.nickname = nickname;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}