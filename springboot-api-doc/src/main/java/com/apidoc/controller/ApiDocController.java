package com.apidoc.controller;

import com.apidoc.example.ExampleGenerator;
import com.apidoc.generator.JsonGenerator;
import com.apidoc.model.ApiDocumentation;
import com.apidoc.parser.ApiParser;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API文档主控制器
 */
@RestController
@RequestMapping("/api-doc")
public class ApiDocController {

    private final ApiParser apiParser;
    private final JsonGenerator jsonGenerator;
    private final ExampleGenerator exampleGenerator;

    public ApiDocController(ApiParser apiParser, JsonGenerator jsonGenerator, ExampleGenerator exampleGenerator) {
        this.apiParser = apiParser;
        this.jsonGenerator = jsonGenerator;
        this.exampleGenerator = exampleGenerator;
    }

    /**
     * 获取完整API文档
     */
    @GetMapping("/documentation")
    public ApiDocumentation getDocumentation(@RequestParam(defaultValue = "all") String environment) {
        return apiParser.parseAll(environment);
    }

    /**
     * 获取API文档JSON格式
     */
    @GetMapping("/documentation.json")
    public String getDocumentationJson(@RequestParam(defaultValue = "all") String environment) {
        ApiDocumentation doc = apiParser.parseAll(environment);
        return jsonGenerator.toPrettyJson(doc);
    }

    /**
     * 生成示例数据
     */
    @PostMapping("/example")
    public Object generateExample(@RequestBody Map<String, Object> request) {
        try {
            String className = (String) request.get("className");
            Boolean realistic = (Boolean) request.getOrDefault("realistic", true);
            String scenario = (String) request.getOrDefault("scenario", "default");

            Class<?> clazz = Class.forName(className);
            return exampleGenerator.generateExample(clazz, realistic, scenario);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "无法生成示例数据: " + e.getMessage());
            return error;
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "API Documentation Tool");
        result.put("version", "1.0.0");
        return result;
    }
}