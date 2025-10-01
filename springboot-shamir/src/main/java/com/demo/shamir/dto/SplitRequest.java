package com.demo.shamir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拆分请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitRequest {
    private String secret;          // 原始密钥
    private Integer totalShares;    // 总份额数 (n)
    private Integer threshold;      // 门限值 (t)
}