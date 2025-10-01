package com.demo.shamir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombineResponse {
    private String secret;          // 恢复的密钥
    private String message;         // 提示信息
    private boolean success;        // 是否成功
}