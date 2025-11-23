package com.example.sign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * API 安全配置属性
 *
 */
@Data
@Component
@ConfigurationProperties(prefix = "api.security")
public class ApiSecurityProperties {

    /**
     * 是否启用签名验证
     */
    private boolean enabled = true;

    /**
     * 时间戳容忍度（秒）
     */
    private long timeTolerance = 300; // 5分钟

    /**
     * API 密钥映射
     */
    private Map<String, String> apiKeys = new HashMap<>();

    /**
     * 签名算法
     */
    private String algorithm = "HMAC-SHA256";

    /**
     * 是否启用请求日志
     */
    private boolean enableRequestLog = true;

    /**
     * 是否启用响应日志
     */
    private boolean enableResponseLog = false;

    /**
     * 获取指定 API Key 对应的密钥
     *
     * @param apiKey API Key
     * @return 密钥
     */
    public String getApiSecret(String apiKey) {
        return apiKeys.get(apiKey);
    }

    /**
     * 添加 API Key 和密钥
     *
     * @param apiKey API Key
     * @param secret 密钥
     */
    public void addApiKey(String apiKey, String secret) {
        apiKeys.put(apiKey, secret);
    }

    /**
     * 移除 API Key
     *
     * @param apiKey API Key
     * @return 是否移除成功
     */
    public boolean removeApiKey(String apiKey) {
        return apiKeys.remove(apiKey) != null;
    }

    /**
     * 检查 API Key 是否存在
     *
     * @param apiKey API Key
     * @return 是否存在
     */
    public boolean containsApiKey(String apiKey) {
        return apiKeys.containsKey(apiKey);
    }
}