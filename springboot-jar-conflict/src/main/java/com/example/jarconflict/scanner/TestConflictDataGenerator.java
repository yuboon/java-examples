package com.example.jarconflict.scanner;

import com.example.jarconflict.model.JarInfo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TestConflictDataGenerator {

    public List<JarInfo> generateTestConflictData() {
        // 模拟冲突的JAR包数据，用于测试
        JarInfo jar1 = new JarInfo();
        jar1.setName("slf4j-api");
        jar1.setVersion("1.7.36");
        jar1.setPath("/test/slf4j-api-1.7.36.jar");
        jar1.setSize("41 KB");
        jar1.setClasses(Arrays.asList(
            "org.slf4j.Logger",
            "org.slf4j.LoggerFactory",
            "org.slf4j.MDC"
        ));

        JarInfo jar2 = new JarInfo();
        jar2.setName("slf4j-api");
        jar2.setVersion("2.0.9");
        jar2.setPath("/test/slf4j-api-2.0.9.jar");
        jar2.setSize("45 KB");
        jar2.setClasses(Arrays.asList(
            "org.slf4j.Logger",
            "org.slf4j.LoggerFactory",
            "org.slf4j.MDC",
            "org.slf4j.Marker"
        ));

        JarInfo jar3 = new JarInfo();
        jar3.setName("hutool-all");
        jar3.setVersion("5.8.16");
        jar3.setPath("/test/hutool-all-5.8.16.jar");
        jar3.setSize("3.2 MB");
        jar3.setClasses(Arrays.asList(
            "cn.hutool.core.util.StrUtil",
            "cn.hutool.core.collection.CollUtil",
            "cn.hutool.json.JSONUtil"
        ));

        JarInfo jar4 = new JarInfo();
        jar4.setName("hutool-core");
        jar4.setVersion("5.8.19");
        jar4.setPath("/test/hutool-core-5.8.19.jar");
        jar4.setSize("1.4 MB");
        jar4.setClasses(Arrays.asList(
            "cn.hutool.core.util.StrUtil",
            "cn.hutool.core.collection.CollUtil"
        ));

        JarInfo jar5 = new JarInfo();
        jar5.setName("jackson-databind");
        jar5.setVersion("2.15.2");
        jar5.setPath("/test/jackson-databind-2.15.2.jar");
        jar5.setSize("1.5 MB");
        jar5.setClasses(Arrays.asList(
            "com.fasterxml.jackson.databind.ObjectMapper",
            "com.fasterxml.jackson.databind.JsonNode"
        ));

        JarInfo jar6 = new JarInfo();
        jar6.setName("jackson-core");
        jar6.setVersion("2.14.2");
        jar6.setPath("/test/jackson-core-2.14.2.jar");
        jar6.setSize("500 KB");
        jar6.setClasses(Arrays.asList(
            "com.fasterxml.jackson.core.JsonParser",
            "com.fasterxml.jackson.databind.JsonNode"  // 重复类
        ));

        return Arrays.asList(jar1, jar2, jar3, jar4, jar5, jar6);
    }
}