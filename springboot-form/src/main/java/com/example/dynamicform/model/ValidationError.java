package com.example.dynamicform.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 验证错误信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationError {
    private String field;
    private String message;
    private String code;
}