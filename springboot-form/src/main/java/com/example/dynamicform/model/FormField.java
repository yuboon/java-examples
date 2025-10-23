package com.example.dynamicform.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 表单字段信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormField {
    private String name;              // 字段名
    private String type;              // 字段类型
    private String title;             // 字段标题
    private String description;       // 字段描述
    private boolean required;         // 是否必填
    private Integer minLength;        // 最小长度
    private Integer maxLength;        // 最大长度
    private Double minimum;           // 最小值
    private Double maximum;           // 最大值
    private String pattern;           // 正则表达式
    private String format;            // 格式（email, date等）
    private String[] enumValues;      // 枚举值
}