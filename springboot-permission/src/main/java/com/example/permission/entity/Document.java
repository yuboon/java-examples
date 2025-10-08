package com.example.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档实体 - ABAC 资源对象（Object）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    private String id;
    private String title;
    private String content;
    private String ownerId;       // 所有者ID
    private String dept;          // 所属部门
    private String type;          // 文档类型：contract（合同）、report（报告）、public（公开）
    private Integer securityLevel; // 安全级别：1-公开 2-内部 3-机密
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 转换为 ABAC 属性 Map，用于策略匹配
     */
    public Map<String, Object> toAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", this.id);
        attrs.put("ownerId", this.ownerId);
        attrs.put("dept", this.dept);
        attrs.put("type", this.type);
        attrs.put("securityLevel", this.securityLevel);
        return attrs;
    }

    /**
     * 简化构造器
     */
    public Document(String id, String ownerId, String dept) {
        this.id = id;
        this.ownerId = ownerId;
        this.dept = dept;
    }

    public Document(String id, String title, String ownerId, String dept, String type) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.dept = dept;
        this.type = type;
        this.createTime = LocalDateTime.now();
    }
}
