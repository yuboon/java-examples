package com.example.multiport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误响应模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private String serviceType;
    private int port;
    private long timestamp;
    private String path;

    public ErrorResponse(String code, String message, String serviceType, int port, String path) {
        this.code = code;
        this.message = message;
        this.serviceType = serviceType;
        this.port = port;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }
}