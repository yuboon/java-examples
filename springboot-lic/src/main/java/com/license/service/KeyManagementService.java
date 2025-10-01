package com.license.service;

import com.license.util.RSAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * 密钥管理服务
 */
@Service
public class KeyManagementService {

    private static final Logger logger = LoggerFactory.getLogger(KeyManagementService.class);

    @Autowired
    private RSAUtil rsaUtil;

    // 内存中存储密钥（实际项目中应该从配置文件或密钥库加载）
    private PrivateKey cachedPrivateKey;
    private PublicKey cachedPublicKey;

    /**
     * 生成新的密钥对
     */
    public Map<String, String> generateKeyPair() throws Exception {
        KeyPair keyPair = rsaUtil.generateKeyPair();

        String privateKeyPem = rsaUtil.privateKeyToPem(keyPair.getPrivate());
        String publicKeyPem = rsaUtil.publicKeyToPem(keyPair.getPublic());

        // 缓存密钥
        this.cachedPrivateKey = keyPair.getPrivate();
        this.cachedPublicKey = keyPair.getPublic();

        Map<String, String> result = new HashMap<>();
        result.put("privateKey", privateKeyPem);
        result.put("publicKey", publicKeyPem);

        logger.info("新密钥对生成成功");
        return result;
    }

    /**
     * 加载私钥
     */
    public void loadPrivateKey(String privateKeyPem) throws Exception {
        this.cachedPrivateKey = rsaUtil.loadPrivateKeyFromPem(privateKeyPem);
        logger.info("私钥加载成功");
    }

    /**
     * 加载公钥
     */
    public void loadPublicKey(String publicKeyPem) throws Exception {
        this.cachedPublicKey = rsaUtil.loadPublicKeyFromPem(publicKeyPem);
        logger.info("公钥加载成功");
    }

    /**
     * 获取缓存的私钥
     */
    public PrivateKey getCachedPrivateKey() {
        return cachedPrivateKey;
    }

    /**
     * 获取缓存的公钥
     */
    public PublicKey getCachedPublicKey() {
        return cachedPublicKey;
    }

    /**
     * 检查密钥是否已加载
     */
    public boolean isKeysLoaded() {
        return cachedPrivateKey != null && cachedPublicKey != null;
    }
}