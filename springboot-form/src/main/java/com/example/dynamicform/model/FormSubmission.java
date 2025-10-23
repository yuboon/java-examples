package com.example.dynamicform.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表单提交数据实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormSubmission {
    private Long id;                  // 提交ID
    private String schemaId;          // 关联的表单Schema
    private String formData;          // 用户提交的JSON数据
    private String submitterId;       // 提交者ID
    private String status;            // 提交状态：pending, approved, rejected
    private LocalDateTime submittedAt; // 提交时间
}