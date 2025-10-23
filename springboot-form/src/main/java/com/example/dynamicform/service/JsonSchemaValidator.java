package com.example.dynamicform.service;

import com.example.dynamicform.model.ValidationResult;
import com.example.dynamicform.model.ValidationError;
import com.example.dynamicform.model.FormSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON Schema验证核心组件
 */
@Service
@Slf4j
public class JsonSchemaValidator {

    private final ObjectMapper objectMapper;
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    private final FormSchemaService schemaService;

    @Autowired
    public JsonSchemaValidator(ObjectMapper objectMapper, FormSchemaService schemaService) {
        this.objectMapper = objectMapper;
        this.schemaService = schemaService;
    }

    /**
     * 验证表单数据
     */
    public ValidationResult validate(String schemaId, JsonNode data) {
        try {
            // 获取schema定义
            FormSchema formSchema = schemaService.getSchema(schemaId);
            if (formSchema == null) {
                return ValidationResult.failed("Schema not found: " + schemaId);
            }

            JsonNode schemaNode = objectMapper.readTree(formSchema.getSchemaDefinition());

            // 过滤掉空的可选字段，只保留有值的字段和必填字段
            ObjectNode filteredData = filterOptionalFields(data, schemaNode);

            JsonSchema schema = getSchema(schemaId);
            Set<ValidationMessage> validationMessages = schema.validate(filteredData);

            if (validationMessages.isEmpty()) {
                return ValidationResult.success();
            }

            List<ValidationError> errors = new ArrayList<>();
            for (ValidationMessage msg : validationMessages) {
                // 过滤掉由于可选字段为空导致的验证错误
                String path = msg.getPath();
                String fieldName = extractFieldName(path);

                if (isEmptyOptionalFieldError(fieldName, data, schemaNode)) {
                    continue; // 跳过空的可选字段错误
                }

                errors.add(ValidationError.builder()
                    .field(path)
                    .message(msg.getMessage())
                    .code(msg.getType())
                    .build());
            }

            return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();

        } catch (Exception e) {
            log.error("Schema validation failed for schemaId: {}", schemaId, e);
            return ValidationResult.failed("Schema验证失败: " + e.getMessage());
        }
    }

    /**
     * 过滤掉空的可选字段
     */
    private ObjectNode filterOptionalFields(JsonNode data, JsonNode schemaNode) {
        ObjectNode filtered = objectMapper.createObjectNode();
        JsonNode required = schemaNode.get("required");
        JsonNode properties = schemaNode.get("properties");

        if (properties == null) {
            return filtered;
        }

        // 获取必填字段列表
        Set<String> requiredFields = new HashSet<>();
        if (required != null && required.isArray()) {
            for (JsonNode field : required) {
                requiredFields.add(field.asText());
            }
        }

        // 复制数据
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            // 特殊处理对象类型字段
            if (fieldValue.isObject()) {
                JsonNode fieldSchema = properties.get(fieldName);
                if (fieldSchema != null && fieldSchema.has("type") && "object".equals(fieldSchema.get("type").asText())) {
                    // 对于对象类型字段，递归过滤其子字段
                    ObjectNode filteredObject = filterObjectFields(fieldValue, fieldSchema, requiredFields.contains(fieldName));
                    if (!filteredObject.isEmpty() || requiredFields.contains(fieldName)) {
                        filtered.set(fieldName, filteredObject);
                    }
                    continue;
                }
            }

            // 如果是必填字段或者字段有值，则保留
            if (requiredFields.contains(fieldName) || !isEmptyValue(fieldValue)) {
                filtered.set(fieldName, fieldValue);
            }
        }

        return filtered;
    }

    /**
     * 过滤对象字段的子字段
     */
    private ObjectNode filterObjectFields(JsonNode objectData, JsonNode objectSchema, boolean isRequired) {
        ObjectNode filteredObject = objectMapper.createObjectNode();
        JsonNode properties = objectSchema.get("properties");

        if (properties == null || !objectData.isObject()) {
            return filteredObject;
        }

        // 对象字段没有子字段的必填要求（因为对象本身不是必填的）
        // 但需要检查子字段是否有值

        Iterator<Map.Entry<String, JsonNode>> subFields = objectData.fields();
        while (subFields.hasNext()) {
            Map.Entry<String, JsonNode> subEntry = subFields.next();
            String subFieldName = subEntry.getKey();
            JsonNode subFieldValue = subEntry.getValue();

            // 保留有值的子字段
            if (!isEmptyValue(subFieldValue)) {
                filteredObject.set(subFieldName, subFieldValue);
            }
        }

        return filteredObject;
    }

    /**
     * 检查是否为空值
     */
    private boolean isEmptyValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return true;
        }
        if (value.isTextual() && value.asText().trim().isEmpty()) {
            return true;
        }
        if (value.isArray() && value.size() == 0) {
            return true;
        }
        if (value.isObject() && value.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * 从路径中提取字段名
     */
    private String extractFieldName(String path) {
        if (path.startsWith("$.") || path.startsWith("/")) {
            path = path.substring(2);
        }
        return path.split("\\.")[0].split("/")[0];
    }

    /**
     * 检查是否是空的可选字段错误
     */
    private boolean isEmptyOptionalFieldError(String fieldName, JsonNode originalData, JsonNode schemaNode) {
        // 检查字段是否为必填
        JsonNode required = schemaNode.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode reqField : required) {
                if (reqField.asText().equals(fieldName)) {
                    return false; // 是必填字段，不是空可选字段错误
                }
            }
        }

        // 处理嵌套对象字段路径（如 profile.firstName）
        if (fieldName.contains(".")) {
            String[] pathParts = fieldName.split("\\.");
            String parentField = pathParts[0];
            String childField = pathParts[1];

            // 检查父对象字段是否为必填
            if (required != null && required.isArray()) {
                for (JsonNode reqField : required) {
                    if (reqField.asText().equals(parentField)) {
                        return false; // 父字段是必填的
                    }
                }
            }

            // 检查子字段是否为空
            JsonNode parentValue = originalData.get(parentField);
            if (parentValue != null && parentValue.isObject()) {
                JsonNode childValue = parentValue.get(childField);
                return isEmptyValue(childValue);
            }
            return true; // 父对象不存在或为空
        }

        // 检查字段在原数据中是否为空
        JsonNode fieldValue = originalData.get(fieldName);
        return isEmptyValue(fieldValue);
    }

    /**
     * 验证单个字段
     */
    public ValidationResult validateField(String schemaId, String fieldName, JsonNode fieldValue) {
        try {
            FormSchema formSchema = schemaService.getSchema(schemaId);
            if (formSchema == null) {
                return ValidationResult.failed("Schema not found: " + schemaId);
            }

            JsonNode schemaNode = objectMapper.readTree(formSchema.getSchemaDefinition());
            JsonNode fieldDefinition = getFieldDefinition(schemaNode.get("properties"), fieldName);

            if (fieldDefinition == null) {
                return ValidationResult.success(); // 字段不存在，视为通过
            }

            // 跳过对象类型字段的验证，只验证叶子节点字段
            if (fieldDefinition.has("type") && "object".equals(fieldDefinition.get("type").asText())) {
                return ValidationResult.success(); // 对象类型字段不需要单独验证
            }

            // 创建简单的字段验证 schema
            ObjectNode fieldSchema = createSimpleFieldSchema(fieldDefinition);

            // 构建临时数据对象，字段名简化为 "field"
            ObjectNode data = objectMapper.createObjectNode();
            data.set("field", fieldValue);

            // 使用字段专用的 schema 进行验证
            JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(fieldSchema);

            Set<ValidationMessage> validationMessages = jsonSchema.validate(data);

            if (validationMessages.isEmpty()) {
                return ValidationResult.success();
            }

            List<ValidationError> errors = new ArrayList<>();
            for (ValidationMessage msg : validationMessages) {
                // 将路径中的 "field" 替换为实际的字段名
                String path = msg.getPath().replace("$.field", "$." + fieldName);
                String message = msg.getMessage();

                // 特殊处理邮箱格式错误，简化错误信息
                if (message.contains("email") || message.contains("RFC 5321")) {
                    message = "邮箱格式不正确";
                }

                errors.add(ValidationError.builder()
                    .field(path)
                    .message(message)
                    .code(msg.getType())
                    .build());
            }

            return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();

        } catch (Exception e) {
            log.error("Field validation failed for schemaId: {}, field: {}", schemaId, fieldName, e);
            return ValidationResult.failed("字段验证失败: " + e.getMessage());
        }
    }

    /**
     * 创建简单的字段验证 schema
     */
    private ObjectNode createSimpleFieldSchema(JsonNode fieldDefinition) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("field", fieldDefinition);

        schema.set("properties", properties);

        // 如果字段是必填的，添加到 required 数组
        ArrayNode required = objectMapper.createArrayNode();
        required.add("field");
        schema.set("required", required);

        return schema;
    }

    /**
     * 获取字段定义（支持嵌套路径）
     */
    private JsonNode getFieldDefinition(JsonNode properties, String fieldName) {
        if (properties == null) {
            return null;
        }

        String[] pathParts = fieldName.split("\\.");
        JsonNode current = properties;

        for (int i = 0; i < pathParts.length - 1; i++) {
            if (current.has(pathParts[i])) {
                JsonNode node = current.get(pathParts[i]);
                if (node.has("properties")) {
                    current = node.get("properties");
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        String finalFieldName = pathParts[pathParts.length - 1];
        return current.has(finalFieldName) ? current.get(finalFieldName) : null;
    }

    /**
     * 获取缓存的JsonSchema
     */
    private JsonSchema getSchema(String schemaId) throws Exception {
        return schemaCache.computeIfAbsent(schemaId, k -> {
            try {
                FormSchema formSchema = schemaService.getSchema(k);
                if (formSchema == null) {
                    throw new IllegalArgumentException("Schema not found: " + k);
                }

                JsonNode schemaNode = objectMapper.readTree(formSchema.getSchemaDefinition());
                return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schemaNode);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse schema", e);
            }
        });
    }

    /**
     * 清除缓存
     */
    public void clearCache(String schemaId) {
        schemaCache.remove(schemaId);
        log.info("Cleared cache for schema: {}", schemaId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        schemaCache.clear();
        log.info("Cleared all schema cache");
    }
}