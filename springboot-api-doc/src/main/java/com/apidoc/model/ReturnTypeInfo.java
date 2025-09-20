package com.apidoc.model;

import java.util.List;

/**
 * 返回类型信息模型
 */
public class ReturnTypeInfo {
    private String type;
    private String description;
    private Object example;
    private List<FieldInfo> fields; // 字段列表

    public ReturnTypeInfo() {}

    public ReturnTypeInfo(String type) {
        this.type = type;
    }

    public ReturnTypeInfo(String type, String description) {
        this.type = type;
        this.description = description;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Object getExample() { return example; }
    public void setExample(Object example) { this.example = example; }

    public List<FieldInfo> getFields() { return fields; }
    public void setFields(List<FieldInfo> fields) { this.fields = fields; }
}