package com.apidoc.model;

import java.util.List;

/**
 * 字段信息模型
 */
public class FieldInfo {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private String example;
    private List<FieldInfo> children; // 用于嵌套对象

    public FieldInfo() {}

    public FieldInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public FieldInfo(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
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

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public List<FieldInfo> getChildren() { return children; }
    public void setChildren(List<FieldInfo> children) { this.children = children; }
}