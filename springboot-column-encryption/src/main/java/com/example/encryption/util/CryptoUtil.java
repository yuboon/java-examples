package com.example.encryption.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加密工具类
 *
 * 特性：
 * - 使用 AES-GCM 算法，提供强加密和完整性验证
 * - 每次加密使用随机 IV，确保相同明文产生不同密文
 * - 密文格式: Base64(IV):Base64(EncryptedData)
 * - 支持密钥热更新
 */
@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // GCM 推荐的 IV 长度
    private static final int GCM_TAG_LENGTH = 128; // GCM 认证标签长度

    // 默认密钥 - 实际项目中应该从安全的密钥管理系统获取
    private static final String DEFAULT_KEY = "MySecretKey12345MySecretKey12345";

    private static SecretKeySpec secretKey;

    static {
        initKey(DEFAULT_KEY);
    }

    /**
     * 初始化密钥
     */
    public static void initKey(String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = base64Key.getBytes(StandardCharsets.UTF_8);
            // 确保密钥长度为 32 字节 (256 位)
            byte[] finalKeyBytes = new byte[32];
            System.arraycopy(keyBytes, 0, finalKeyBytes, 0, Math.min(keyBytes.length, 32));
            secretKey = new SecretKeySpec(finalKeyBytes, "AES");
        } catch (Exception e) {
            log.error("密钥初始化失败", e);
            throw new RuntimeException("密钥初始化失败", e);
        }
    }

    /**
     * 加密明文字符串
     *
     * @param plainText 明文
     * @return 格式为 "Base64(IV):Base64(EncryptedData)" 的密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 生成随机 IV
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // 执行加密
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合 IV 和加密数据，使用 Base64 编码
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedData);

            return ivBase64 + ":" + encryptedBase64;

        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密密文字符串
     *
     * @param cipherText 格式为 "Base64(IV):Base64(EncryptedData)" 的密文
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            // 分离 IV 和加密数据
            String[] parts = cipherText.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("密文格式错误");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // 执行解密
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 检查字符串是否为加密格式
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains(":") && text.split(":").length == 2;
    }

    /**
     * 热更新密钥
     */
    public static void updateKey(String newKey) {
        log.info("正在更新加密密钥...");
        initKey(newKey);
        log.info("加密密钥更新完成");
    }

    /**
     * 生成随机密钥
     */
    public static String generateRandomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}