package com.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class AESUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private final SecretKeySpec secretKey;
    
    public AESUtil(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("AES key cannot be blank");
        }
        // 确保密钥长度为16字节
        String normalizedKey = normalizeKey(key);
        this.secretKey = new SecretKeySpec(normalizedKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }
    
    private String normalizeKey(String key) {
        if (key.length() == 16) {
            return key;
        } else if (key.length() < 16) {
            return String.format("%-16s", key).replace(' ', '0');
        } else {
            return key.substring(0, 16);
        }
    }
    
    /**
     * 加密
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * 解密
     */
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * 生成随机AES密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(128, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("生成密钥失败", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }
}