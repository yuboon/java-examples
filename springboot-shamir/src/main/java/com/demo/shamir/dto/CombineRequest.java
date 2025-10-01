package com.demo.shamir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 恢复请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombineRequest {
    private List<String> shares;    // 密钥份额列表
}