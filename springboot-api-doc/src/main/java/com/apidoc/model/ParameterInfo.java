package com.apidoc.model;

import java.util.List;

/**
 * 参数信息模型
 */
public class ParameterInfo {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private String paramType; // query, path, body, header
    private String example;
    private String defaultValue;
    private List<FieldInfo> fields; // 对象类型参数的字段列表

    public ParameterInfo() {}

    public ParameterInfo(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getParamType() { return paramType; }
    public void setParamType(String paramType) { this.paramType = paramType; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public List<FieldInfo> getFields() { return fields; }
    public void setFields(List<FieldInfo> fields) { this.fields = fields; }
}