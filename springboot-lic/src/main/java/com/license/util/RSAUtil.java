package com.license.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA密钥管理工具类
 */
@Component
public class RSAUtil {

    private static final Logger logger = LoggerFactory.getLogger(RSAUtil.class);
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成RSA密钥对
     */
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * 将私钥转换为PEM格式字符串
     */
    public String privateKeyToPem(PrivateKey privateKey) {
        String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" +
                formatBase64String(encoded) +
                "\n-----END PRIVATE KEY-----";
    }

    /**
     * 将公钥转换为PEM格式字符串
     */
    public String publicKeyToPem(PublicKey publicKey) {
        String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" +
                formatBase64String(encoded) +
                "\n-----END PUBLIC KEY-----";
    }

    /**
     * 从PEM格式字符串加载私钥
     */
    public PrivateKey loadPrivateKeyFromPem(String pemContent) throws Exception {
        String privateKeyPEM = pemContent
                .replaceAll("-----\\w+ PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 从PEM格式字符串加载公钥
     */
    public PublicKey loadPublicKeyFromPem(String pemContent) throws Exception {
        String publicKeyPEM = pemContent
                .replaceAll("-----\\w+ PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * 使用私钥对数据进行签名
     */
    public String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }

    /**
     * 使用公钥验证签名
     */
    public boolean verify(String data, String signatureBase64, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }

    /**
     * 格式化Base64字符串，每64个字符换行
     */
    private String formatBase64String(String base64) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < base64.length(); i += 64) {
            formatted.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }
        return formatted.toString().trim();
    }
}