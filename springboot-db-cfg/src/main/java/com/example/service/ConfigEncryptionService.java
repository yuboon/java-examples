package com.example.service;

import com.example.utils.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigEncryptionService {
    
    private final AESUtil aesUtil;
    
    public ConfigEncryptionService(@Value("${app.config.encryption.key:defaultConfigKey123}") String encryptionKey) {
        // 加密密钥从环境变量或密钥管理服务获取
        this.aesUtil = new AESUtil(getEncryptionKey(encryptionKey));
    }
    
    private String getEncryptionKey(String defaultKey) {
        // 优先从环境变量获取
        String key = System.getenv("CONFIG_ENCRYPTION_KEY");
        if (key == null || key.trim().isEmpty()) {
            key = System.getProperty("config.encryption.key");
        }
        if (key == null || key.trim().isEmpty()) {
            key = defaultKey;
        }
        return key;
    }
    
    /**
     * 加密敏感配置
     */
    public String encryptSensitiveConfig(String plainText) {
        try {
            return aesUtil.encrypt(plainText);
        } catch (Exception e) {
            log.error("配置加密失败", e);
            throw new RuntimeException("Configuration encryption failed", e);
        }
    }
    
    /**
     * 解密敏感配置
     */
    public String decryptSensitiveConfig(String encryptedText) {
        try {
            return aesUtil.decrypt(encryptedText);
        } catch (Exception e) {
            log.error("配置解密失败", e);
            throw new RuntimeException("Configuration decryption failed", e);
        }
    }
    
    /**
     * 判断配置是否为敏感信息
     */
    public boolean isSensitiveConfig(String configKey) {
        if (configKey == null) {
            return false;
        }
        String lowerKey = configKey.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("token") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("auth") ||
               lowerKey.contains("private") ||
               lowerKey.endsWith(".pwd") ||
               lowerKey.endsWith(".pass");
    }
    
    /**
     * 解密敏感配置 - 兼容方法名
     */
    public String decrypt(String encryptedText) {
        return decryptSensitiveConfig(encryptedText);
    }
    
    /**
     * 加密敏感配置 - 兼容方法名  
     */
    public String encrypt(String plainText) {
        return encryptSensitiveConfig(plainText);
    }
}