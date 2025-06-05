package com.example.livestream.dto;

import lombok.Data;

@Data
public class SrsCallbackDto {
    private String app;
    private String stream;
    private String token;
    private String expire;
    private String file;
    // 其他可能的回调参数
    private String clientId;
    private String ip;
    private String vhost;
    private String param;
    private String tcUrl;
    private String domain;
}