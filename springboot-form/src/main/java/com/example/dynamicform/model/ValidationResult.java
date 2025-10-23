package com.example.dynamicform.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证结果封装类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;

    public static ValidationResult failed(String message) {
        return ValidationResult.builder()
            .valid(false)
            .errors(List.of(ValidationError.builder()
                .field("_global")
                .message(message)
                .code("VALIDATION_FAILED")
                .build()))
            .build();
    }

    public static ValidationResult success() {
        return ValidationResult.builder()
            .valid(true)
            .errors(List.of())
            .build();
    }
}