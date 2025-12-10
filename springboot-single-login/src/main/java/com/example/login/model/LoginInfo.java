package com.example.login.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginInfo {
    private String ip;           // IP地址
    private String device;       // 设备信息
    private String userAgent;    // User-Agent
    private long loginTime;      // 登录时间
}