package com.example.diff.service;

import com.example.diff.model.DiffResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;

@Service
public class YamlDiffService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * 比对 YAML 配置
     * 先解析为 JSON 树，再转为规范格式进行比对
     */
    public DiffResult compareYaml(String originalYaml, String revisedYaml) throws Exception {
        // 解析 YAML
        JsonNode originalTree = yamlMapper.readTree(originalYaml);
        JsonNode revisedTree = yamlMapper.readTree(revisedYaml);

        // 转为规范 JSON 字符串
        String originalJson = jsonMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(originalTree);
        String revisedJson = jsonMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(revisedTree);

        // 使用 DiffService 进行文本比对
        DiffService diffService = new DiffService();
        return diffService.compareConfigs(originalJson, revisedJson);
    }
}
