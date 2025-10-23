package com.example.dynamicform.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表单Schema定义实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormSchema {
    private String schemaId;          // 表单唯一标识
    private String name;              // 表单名称
    private String description;       // 表单描述
    private String schemaDefinition;  // JSON Schema定义
    private String category;          // 表单分类
    private Integer version;          // 版本号
    private Boolean active;           // 是否启用
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}