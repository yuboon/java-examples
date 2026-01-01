package com.example.diff.service;

import com.example.diff.model.PropertiesDiffResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

@Service
public class PropertiesDiffService {

    /**
     * 智能比对 Properties 配置
     */
    public PropertiesDiffResult compareProperties(String originalContent, String revisedContent) {
        Properties original = parseProperties(originalContent);
        Properties revised = parseProperties(revisedContent);

        PropertiesDiffResult result = new PropertiesDiffResult();

        // 找出删除的 key
        Set<String> removedKeys = new HashSet<>(original.stringPropertyNames());
        removedKeys.removeAll(revised.stringPropertyNames());
        result.setRemovedKeys(removedKeys);

        // 找出新增的 key
        Set<String> addedKeys = new HashSet<>(revised.stringPropertyNames());
        addedKeys.removeAll(original.stringPropertyNames());
        result.setAddedKeys(addedKeys);

        // 找出修改的 key
        Set<String> modifiedKeys = new HashSet<>();
        for (String key : original.stringPropertyNames()) {
            if (revised.containsKey(key)) {
                String oldValue = original.getProperty(key);
                String newValue = revised.getProperty(key);
                if (!Objects.equals(oldValue, newValue)) {
                    modifiedKeys.add(key);
                    result.addModifiedKey(key, oldValue, newValue);
                }
            }
        }
        result.setModifiedKeys(modifiedKeys);

        return result;
    }

    private Properties parseProperties(String content) {
        Properties props = new Properties();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes())) {
            props.load(bis);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse properties", e);
        }
        return props;
    }
}
