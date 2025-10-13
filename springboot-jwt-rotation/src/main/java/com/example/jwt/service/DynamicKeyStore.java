package com.example.jwt.service;

import com.example.jwt.model.KeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态密钥存储管理器
 * 负责密钥的生成、存储、获取和清理
 */
@Service
public class DynamicKeyStore {

    private static final Logger logger = LoggerFactory.getLogger(DynamicKeyStore.class);
    private static final String KEY_ID_PREFIX = "key-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${jwt.key-size:2048}")
    private int keySize;

    // 线程安全的密钥存储
    private final Map<String, KeyInfo> keyStore = new ConcurrentHashMap<>();

    // 当前活跃密钥ID
    private volatile String currentKeyId;

    /**
     * 初始化密钥存储，创建一个初始密钥
     */
    public void initialize() {
        logger.info("初始化动态密钥存储...");
        generateNewKeyPair();
        logger.info("密钥存储初始化完成，当前密钥ID: {}", currentKeyId);
    }

    /**
     * 生成新的RSA密钥对
     */
    public String generateNewKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String keyId = generateKeyId();
            KeyInfo keyInfo = new KeyInfo(keyId, keyPair);

            // 如果是第一个密钥，直接设为当前密钥
            if (currentKeyId == null) {
                currentKeyId = keyId;
            } else {
                // 将旧密钥标记为非活跃，但保留用于验证
                KeyInfo oldKeyInfo = keyStore.get(currentKeyId);
                if (oldKeyInfo != null) {
                    oldKeyInfo.setActive(false);
                }
                currentKeyId = keyId;
            }

            keyStore.put(keyId, keyInfo);

            logger.info("生成新密钥对: {} (密钥长度: {} bits)", keyId, keySize);
            return keyId;

        } catch (NoSuchAlgorithmException e) {
            logger.error("生成RSA密钥对失败", e);
            throw new RuntimeException("无法生成RSA密钥对", e);
        }
    }

    /**
     * 获取当前活跃的密钥信息
     */
    public KeyInfo getCurrentKey() {
        // 检查是否已初始化
        if (currentKeyId == null) {
            logger.error("密钥存储未初始化！正在初始化...");
            initialize();
        }

        KeyInfo keyInfo = keyStore.get(currentKeyId);
        if (keyInfo == null) {
            logger.error("当前密钥不存在！生成新密钥...");
            generateNewKeyPair();
            keyInfo = keyStore.get(currentKeyId);
        }

        if (keyInfo != null) {
            // 更新最后使用时间
            keyInfo.setLastUsed(LocalDateTime.now());
        }

        return keyInfo;
    }

    /**
     * 根据密钥ID获取密钥信息
     */
    public KeyInfo getKey(String keyId) {
        KeyInfo keyInfo = keyStore.get(keyId);
        if (keyInfo != null) {
            // 更新最后使用时间
            keyInfo.setLastUsed(LocalDateTime.now());
        }
        return keyInfo;
    }

    /**
     * 清理过期的密钥
     * @param gracePeriodDays 宽限期天数
     */
    public List<String> cleanupExpiredKeys(int gracePeriodDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(gracePeriodDays);
        List<String> removedKeys = new ArrayList<>();

        keyStore.entrySet().removeIf(entry -> {
            KeyInfo keyInfo = entry.getValue();
            boolean isExpired = keyInfo.getCreatedAt().isBefore(cutoffTime);

            // 不能删除当前正在使用的密钥
            if (isExpired && !keyInfo.getKeyId().equals(currentKeyId)) {
                removedKeys.add(entry.getKey());
                logger.info("清理过期密钥: {} (创建时间: {})",
                    keyInfo.getKeyId(), keyInfo.getCreatedAt());
                return true;
            }
            return false;
        });

        return removedKeys;
    }

    /**
     * 获取所有密钥信息（用于监控和调试）
     */
    public Map<String, KeyInfo> getAllKeys() {
        return new HashMap<>(keyStore);
    }

    /**
     * 获取密钥存储统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalKeys", keyStore.size());
        stats.put("currentKeyId", currentKeyId);

        List<Map<String, Object>> keyDetails = keyStore.values().stream()
            .map(keyInfo -> {
                Map<String, Object> detail = new HashMap<>();
                detail.put("keyId", keyInfo.getKeyId());
                detail.put("createdAt", keyInfo.getCreatedAt());
                detail.put("lastUsed", keyInfo.getLastUsed());
                detail.put("isActive", keyInfo.isActive());
                detail.put("isCurrent", keyInfo.getKeyId().equals(currentKeyId));
                return detail;
            })
            .collect(Collectors.toList());

        stats.put("keys", keyDetails);
        return stats;
    }

    /**
     * 生成密钥ID
     */
    private String generateKeyId() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
        return KEY_ID_PREFIX + dateStr + "-" + timestamp;
    }
}