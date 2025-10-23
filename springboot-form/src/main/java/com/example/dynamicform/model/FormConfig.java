package com.example.dynamicform.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单配置信息（用于前端渲染）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormConfig {
    private String schemaId;
    private String name;
    private String description;
    private JsonNode schema;
    private List<FormField> fields;
}