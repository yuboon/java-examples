package com.example.dynamicform.controller;

import com.example.dynamicform.model.*;
import com.example.dynamicform.service.FormSchemaService;
import com.example.dynamicform.service.FormSubmissionService;
import com.example.dynamicform.service.JsonSchemaValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 动态表单管理控制器
 */
@RestController
@RequestMapping("/api/forms")
@Slf4j
@CrossOrigin(origins = "*")
public class DynamicFormController {

    private final FormSchemaService schemaService;
    private final JsonSchemaValidator validator;
    private final FormSubmissionService submissionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DynamicFormController(FormSchemaService schemaService,
                               JsonSchemaValidator validator,
                               FormSubmissionService submissionService,
                               ObjectMapper objectMapper) {
        this.schemaService = schemaService;
        this.validator = validator;
        this.submissionService = submissionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取表单配置（用于前端渲染）
     */
    @GetMapping("/{schemaId}/config")
    public ResponseEntity<ApiResponse<FormConfig>> getFormConfig(@PathVariable String schemaId) {
        try {
            FormSchema schema = schemaService.getActiveSchema(schemaId);
            if (schema == null) {
                return ResponseEntity.ok(ApiResponse.error("表单不存在或已禁用"));
            }

            FormConfig config = buildFormConfig(schema);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("Failed to get form config for schemaId: {}", schemaId, e);
            return ResponseEntity.ok(ApiResponse.error("获取表单配置失败: " + e.getMessage()));
        }
    }

    /**
     * 提交表单数据
     */
    @PostMapping("/{schemaId}/submit")
    public ResponseEntity<ApiResponse<Object>> submitForm(
            @PathVariable String schemaId,
            @RequestBody JsonNode formData) {

        try {
            // 验证表单数据
            ValidationResult result = validator.validate(schemaId, formData);
            if (!result.isValid()) {
                return ResponseEntity.ok(ApiResponse.error("表单验证失败", result.getErrors()));
            }

            // 保存提交数据
            FormSubmission submission = submissionService.saveSubmission(
                schemaId, formData.toString(), "anonymous");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("submissionId", submission.getId());
            responseData.put("status", submission.getStatus());
            responseData.put("submittedAt", submission.getSubmittedAt());

            return ResponseEntity.ok(ApiResponse.success("提交成功", responseData));
        } catch (Exception e) {
            log.error("Failed to submit form for schemaId: {}", schemaId, e);
            return ResponseEntity.ok(ApiResponse.error("提交失败: " + e.getMessage()));
        }
    }

    /**
     * 实时验证单个字段
     */
    @PostMapping("/{schemaId}/validate-field")
    public ResponseEntity<ApiResponse<ValidationResult>> validateField(
            @PathVariable String schemaId,
            @RequestBody FieldValidationRequest request) {

        try {
            ValidationResult result = validator.validateField(
                schemaId, request.getFieldName(), request.getFieldValue());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to validate field for schemaId: {}, field: {}", schemaId, request.getFieldName(), e);
            return ResponseEntity.ok(ApiResponse.error("字段验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有可用的表单Schema
     */
    @GetMapping("/schemas")
    public ResponseEntity<ApiResponse<List<SchemaInfo>>> getAllSchemas() {
        try {
            List<FormSchema> schemas = schemaService.getAllSchemas();
            List<SchemaInfo> schemaInfos = schemas.stream()
                .filter(schema -> Boolean.TRUE.equals(schema.getActive()))
                .map(schema -> SchemaInfo.builder()
                    .schemaId(schema.getSchemaId())
                    .name(schema.getName())
                    .description(schema.getDescription())
                    .category(schema.getCategory())
                    .version(schema.getVersion())
                    .build())
                .toList();

            return ResponseEntity.ok(ApiResponse.success(schemaInfos));
        } catch (Exception e) {
            log.error("Failed to get all schemas", e);
            return ResponseEntity.ok(ApiResponse.error("获取表单列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新的表单Schema
     */
    @PostMapping("/schemas")
    public ResponseEntity<ApiResponse<FormSchema>> createSchema(@RequestBody CreateSchemaRequest request) {
        try {
            // 验证Schema定义的格式有效性
            try {
                objectMapper.readTree(request.getSchemaDefinition());
            } catch (Exception e) {
                return ResponseEntity.ok(ApiResponse.error("JSON Schema格式无效: " + e.getMessage()));
            }

            FormSchema schema = FormSchema.builder()
                .schemaId(request.getSchemaId())
                .name(request.getName())
                .description(request.getDescription())
                .schemaDefinition(request.getSchemaDefinition())
                .category(request.getCategory())
                .build();

            FormSchema savedSchema = schemaService.saveSchema(schema);
            return ResponseEntity.ok(ApiResponse.success("创建成功", savedSchema));
        } catch (Exception e) {
            log.error("Failed to create schema", e);
            return ResponseEntity.ok(ApiResponse.error("创建表单失败: " + e.getMessage()));
        }
    }

    /**
     * 获取表单提交数据
     */
    @GetMapping("/{schemaId}/submissions")
    public ResponseEntity<ApiResponse<List<FormSubmission>>> getSubmissions(@PathVariable String schemaId) {
        try {
            List<FormSubmission> submissions = submissionService.getSubmissionsBySchema(schemaId);
            return ResponseEntity.ok(ApiResponse.success(submissions));
        } catch (Exception e) {
            log.error("Failed to get submissions for schemaId: {}", schemaId, e);
            return ResponseEntity.ok(ApiResponse.error("获取提交数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = submissionService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Failed to get statistics", e);
            return ResponseEntity.ok(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 构建表单配置
     */
    private FormConfig buildFormConfig(FormSchema schema) {
        try {
            log.debug("Building form config for schema: {}", schema.getSchemaId());
            JsonNode schemaNode = objectMapper.readTree(schema.getSchemaDefinition());
            List<FormField> fields = parseFields(schemaNode);

            return FormConfig.builder()
                .schemaId(schema.getSchemaId())
                .name(schema.getName())
                .description(schema.getDescription())
                .schema(schemaNode)
                .fields(fields)
                .build();
        } catch (Exception e) {
            log.error("Failed to build form config for schema: {}", schema.getSchemaId(), e);
            throw new RuntimeException("Failed to build form config: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Schema字段（支持嵌套对象）
     */
    private List<FormField> parseFields(JsonNode schemaNode) {
        List<FormField> fields = new ArrayList<>();
        parseFieldsRecursive(schemaNode, fields, new ArrayList<>());
        return fields;
    }

    /**
     * 递归解析Schema字段
     */
    private void parseFieldsRecursive(JsonNode schemaNode, List<FormField> fields, List<String> parentPath) {
        JsonNode properties = schemaNode.get("properties");
        JsonNode required = schemaNode.get("required");

        if (properties != null) {
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = properties.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldIterator.next();
                String fieldName = entry.getKey();
                JsonNode fieldSchema = entry.getValue();

                // 构建完整的字段路径
                String fullPath = parentPath.isEmpty() ? fieldName : String.join(".", parentPath) + "." + fieldName;

                boolean isRequired = false;
                try {
                    isRequired = required != null && required.isArray() &&
                        Arrays.asList(objectMapper.treeToValue(required, String[].class)).contains(fullPath);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse required fields: {}", fullPath, e);
                }

                FormField field = FormField.builder()
                    .name(fullPath)
                    .type(fieldSchema.has("type") ? fieldSchema.get("type").asText() : "string")
                    .title(fieldSchema.has("title") ? fieldSchema.get("title").asText() : fieldName)
                    .description(fieldSchema.has("description") ? fieldSchema.get("description").asText() : "")
                    .required(isRequired)
                    .build();

                // 如果字段是对象类型，递归解析其属性，但不添加对象字段本身
                if ("object".equals(field.getType())) {
                    List<String> childPath = new ArrayList<>(parentPath);
                    childPath.add(fieldName);
                    parseFieldsRecursive(fieldSchema, fields, childPath);
                } else {
                    // 只添加非对象类型的字段到字段列表
                    parseValidationRules(field, fieldSchema);
                    fields.add(field);
                }
            }
        }
    }

    /**
     * 解析验证规则
     */
    private void parseValidationRules(FormField field, JsonNode fieldSchema) {
        if (fieldSchema.has("minLength")) {
            field.setMinLength(fieldSchema.get("minLength").asInt());
        }
        if (fieldSchema.has("maxLength")) {
            field.setMaxLength(fieldSchema.get("maxLength").asInt());
        }
        if (fieldSchema.has("minimum")) {
            field.setMinimum(fieldSchema.get("minimum").asDouble());
        }
        if (fieldSchema.has("maximum")) {
            field.setMaximum(fieldSchema.get("maximum").asDouble());
        }
        if (fieldSchema.has("pattern")) {
            field.setPattern(fieldSchema.get("pattern").asText());
        }
        if (fieldSchema.has("format")) {
            field.setFormat(fieldSchema.get("format").asText());
        }
        if (fieldSchema.has("enum")) {
            try {
                String[] enumValues = objectMapper.treeToValue(fieldSchema.get("enum"), String[].class);
                field.setEnumValues(enumValues);
            } catch (Exception e) {
                log.warn("Failed to parse enum values for field: {}", field.getName(), e);
            }
        } else if ("array".equals(field.getType()) && fieldSchema.has("items") && fieldSchema.get("items").has("enum")) {
            // 处理数组类型字段的枚举值（在items中定义）
            try {
                String[] enumValues = objectMapper.treeToValue(fieldSchema.get("items").get("enum"), String[].class);
                field.setEnumValues(enumValues);
            } catch (Exception e) {
                log.warn("Failed to parse array enum values for field: {}", field.getName(), e);
            }
        }
    }

    /**
     * 字段验证请求
     */
    public static class FieldValidationRequest {
        private String fieldName;
        private JsonNode fieldValue;

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public JsonNode getFieldValue() { return fieldValue; }
        public void setFieldValue(JsonNode fieldValue) { this.fieldValue = fieldValue; }
    }

    /**
     * Schema信息
     */
    @lombok.Data
    @lombok.Builder
    public static class SchemaInfo {
        private String schemaId;
        private String name;
        private String description;
        private String category;
        private Integer version;
    }

    /**
     * 创建Schema请求
     */
    public static class CreateSchemaRequest {
        private String schemaId;
        private String name;
        private String description;
        private String schemaDefinition;
        private String category;

        // Getters and Setters
        public String getSchemaId() { return schemaId; }
        public void setSchemaId(String schemaId) { this.schemaId = schemaId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSchemaDefinition() { return schemaDefinition; }
        public void setSchemaDefinition(String schemaDefinition) { this.schemaDefinition = schemaDefinition; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}