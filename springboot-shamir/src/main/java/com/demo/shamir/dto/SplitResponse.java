package com.demo.shamir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拆分响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitResponse {
    private String sessionId;       // 会话 ID
    private List<String> shares;    // 密钥份额列表
    private String message;         // 提示信息
}