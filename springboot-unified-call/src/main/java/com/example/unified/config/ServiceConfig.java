package com.example.unified.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务调用协议配置类，从配置文件加载服务与协议的映射关系
 */
@Component
@ConfigurationProperties(prefix = "service") // 绑定配置文件中的 "service" 前缀
@Data
public class ServiceConfig {
    /**
     * 服务协议配置，key：服务名，value：协议类型（rest/rpc）
     * 示例：user-service: rpc，order-service: rest
     */
    private Map<String, String> config = new HashMap<>();

    /**
     * 根据服务名获取协议类型
     * @param serviceName 服务名（如 "user-service"）
     * @return 协议类型（"rest" 或 "rpc"），默认返回 "rest"
     */
    public String getProtocol(String serviceName) {
        return config.getOrDefault(serviceName, "rest"); // 未配置时默认使用 REST
    }
}