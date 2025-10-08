package com.example.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户实体 - ABAC 主体（Subject）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String name;
    private String dept;      // 部门
    private String role;      // 角色（可选）
    private Integer level;    // 级别

    /**
     * 转换为 ABAC 属性 Map，用于策略匹配
     */
    public Map<String, Object> toAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", this.id);
        attrs.put("name", this.name);
        attrs.put("dept", this.dept);
        attrs.put("role", this.role);
        attrs.put("level", this.level);
        return attrs;
    }

    /**
     * 简化构造器
     */
    public User(String id, String dept) {
        this.id = id;
        this.dept = dept;
    }

    public User(String id, String name, String dept) {
        this.id = id;
        this.name = name;
        this.dept = dept;
    }
}
