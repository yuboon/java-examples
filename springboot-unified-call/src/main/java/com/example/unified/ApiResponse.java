package com.example.unified;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ApiResponse<T> implements Serializable {

    private String code;       // 状态码
    private String message;    // 消息提示
    private T data;            // 业务数据
    private long timestamp;    // 时间戳

    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 失败响应
    public static <T> ApiResponse<T> fail(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}