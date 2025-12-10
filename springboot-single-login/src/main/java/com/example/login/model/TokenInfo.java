package com.example.login.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Token信息
 */
@Data
@AllArgsConstructor
public class TokenInfo {
    private String token;        // Token值
    private String username;     // 用户名
    private LoginInfo loginInfo; // 登录信息
    private long expireTime;     // 过期时间
}