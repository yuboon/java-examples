package com.example.dynamicform.service;

import com.example.dynamicform.model.FormSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表单Schema管理服务（基于内存Map存储）
 */
@Service
@Slf4j
public class FormSchemaService {

    // 使用内存Map存储Schema定义
    private final Map<String, FormSchema> schemaStore = new ConcurrentHashMap<>();
    private final Map<String, List<FormSchema>> categoryStore = new ConcurrentHashMap<>();

    public FormSchemaService() {
        // 初始化一些示例Schema
        initializeDefaultSchemas();
    }

    /**
     * 获取Schema
     */
    public FormSchema getSchema(String schemaId) {
        return schemaStore.get(schemaId);
    }

    /**
     * 获取激活的Schema
     */
    public FormSchema getActiveSchema(String schemaId) {
        FormSchema schema = schemaStore.get(schemaId);
        return (schema != null && Boolean.TRUE.equals(schema.getActive())) ? schema : null;
    }

    /**
     * 创建或更新Schema
     */
    public FormSchema saveSchema(FormSchema schema) {
        if (schema.getSchemaId() == null || schema.getSchemaId().trim().isEmpty()) {
            throw new IllegalArgumentException("Schema ID cannot be null or empty");
        }

        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        if (schema.getCreatedAt() == null) {
            schema.setCreatedAt(now);
        }
        schema.setUpdatedAt(now);

        // 默认值设置
        if (schema.getActive() == null) {
            schema.setActive(true);
        }
        if (schema.getVersion() == null) {
            schema.setVersion(1);
        }

        // 保存到存储
        schemaStore.put(schema.getSchemaId(), schema);

        // 更新分类索引
        if (schema.getCategory() != null) {
            categoryStore.computeIfAbsent(schema.getCategory(), k -> new ArrayList<>()).add(schema);
        }

        log.info("Saved schema: {} - {}", schema.getSchemaId(), schema.getName());
        return schema;
    }

    /**
     * 删除Schema
     */
    public boolean deleteSchema(String schemaId) {
        FormSchema schema = schemaStore.remove(schemaId);
        if (schema != null && schema.getCategory() != null) {
            List<FormSchema> schemas = categoryStore.get(schema.getCategory());
            if (schemas != null) {
                schemas.removeIf(s -> s.getSchemaId().equals(schemaId));
            }
        }
        log.info("Deleted schema: {}", schemaId);
        return schema != null;
    }

    /**
     * 获取所有Schema
     */
    public List<FormSchema> getAllSchemas() {
        return new ArrayList<>(schemaStore.values());
    }

    /**
     * 根据分类获取Schema
     */
    public List<FormSchema> getSchemasByCategory(String category) {
        return categoryStore.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return categoryStore.keySet();
    }

    /**
     * 初始化默认Schema
     */
    private void initializeDefaultSchemas() {
        // 用户注册表单Schema
        String userRegistrationSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "title": "用户注册表单",
              "description": "新用户注册信息收集表单",
              "required": ["username", "email", "password", "confirmPassword"],
              "properties": {
                "username": {
                  "type": "string",
                  "title": "用户名",
                  "description": "3-20位字母、数字或下划线",
                  "minLength": 3,
                  "maxLength": 20,
                  "pattern": "^[a-zA-Z0-9_]+$"
                },
                "email": {
                  "type": "string",
                  "title": "邮箱地址",
                  "description": "请输入有效的邮箱地址",
                  "format": "email"
                },
                "password": {
                  "type": "string",
                  "title": "密码",
                  "description": "至少8位，包含大小写字母和数字",
                  "minLength": 8,
                  "pattern": "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)[a-zA-Z\\\\d@$!%*?&]{8,}$"
                },
                "confirmPassword": {
                  "type": "string",
                  "title": "确认密码",
                  "description": "请再次输入密码",
                  "minLength": 8
                },
                "profile": {
                  "type": "object",
                  "title": "个人信息",
                  "properties": {
                    "firstName": {
                      "type": "string",
                      "title": "名字",
                      "maxLength": 50
                    },
                    "lastName": {
                      "type": "string",
                      "title": "姓氏",
                      "maxLength": 50
                    },
                    "phone": {
                      "type": "string",
                      "title": "手机号码",
                      "description": "请输入11位手机号码",
                      "pattern": "^1[3-9]\\\\d{9}$"
                    },
                    "birthDate": {
                      "type": "string",
                      "format": "date",
                      "title": "出生日期"
                    }
                  }
                },
                "preferences": {
                  "type": "array",
                  "title": "兴趣偏好",
                  "description": "请选择您的兴趣爱好",
                  "items": {
                    "type": "string",
                    "enum": ["technology", "sports", "music", "reading", "travel", "food"]
                  },
                  "uniqueItems": true
                },
                "newsletter": {
                  "type": "boolean",
                  "title": "订阅新闻通讯",
                  "description": "是否接收我们的最新资讯",
                  "default": false
                }
              }
            }
            """;

        FormSchema userRegSchema = FormSchema.builder()
            .schemaId("user-registration")
            .name("用户注册表单")
            .description("新用户注册信息收集表单")
            .schemaDefinition(userRegistrationSchema)
            .category("用户管理")
            .version(1)
            .active(true)
            .build();

        saveSchema(userRegSchema);

        // 满意度调查表单Schema
        String satisfactionSurveySchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "title": "客户满意度调查",
              "description": "请对我们的服务进行评价",
              "required": ["overallRating", "service"],
              "properties": {
                "overallRating": {
                  "type": "integer",
                  "title": "总体满意度",
                  "description": "请为我们的整体服务打分（1-5分）",
                  "minimum": 1,
                  "maximum": 5
                },
                "service": {
                  "type": "string",
                  "title": "服务体验",
                  "description": "您对我们服务的整体评价如何？",
                  "enum": ["非常满意", "满意", "一般", "不满意", "非常不满意"]
                },
                "recommendation": {
                  "type": "boolean",
                  "title": "推荐意愿",
                  "description": "您是否愿意向朋友推荐我们的服务？"
                },
                "feedback": {
                  "type": "string",
                  "title": "详细反馈",
                  "description": "请告诉我们您的想法和建议",
                  "maxLength": 1000
                },
                "contactEmail": {
                  "type": "string",
                  "title": "联系邮箱（可选）",
                  "description": "如需我们回复，请留下邮箱",
                  "format": "email"
                }
              }
            }
            """;

        FormSchema surveySchema = FormSchema.builder()
            .schemaId("satisfaction-survey")
            .name("客户满意度调查")
            .description("客户满意度调查表单")
            .schemaDefinition(satisfactionSurveySchema)
            .category("问卷调查")
            .version(1)
            .active(true)
            .build();

        saveSchema(surveySchema);

        log.info("Initialized {} default schemas", schemaStore.size());
    }
}