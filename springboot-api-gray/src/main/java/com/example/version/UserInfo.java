package com.example.version;

import lombok.Data;

/**
     * 用户信息类，用于灰度发布决策
     */
    @Data
    public class UserInfo {
        private String userId;
        private String[] groups;
        private String region;
    }