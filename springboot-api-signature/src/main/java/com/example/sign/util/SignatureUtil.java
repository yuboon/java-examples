package com.example.sign.util;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * HMAC-SHA256 签名工具类
 *
 */
public class SignatureUtil {

    /**
     * 生成签名
     *
     * @param params 请求参数
     * @param timestamp 时间戳
     * @param secret 密钥
     * @return 签名字符串
     */
    public static String generateSignature(Map<String, Object> params, String timestamp, String secret) {
        // 1. 参数排序
        String sortedParams = sortParams(params);

        // 2. 构建待签名字符串
        String dataToSign = timestamp + sortedParams;

        // 3. HMAC-SHA256 加密
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        byte[] digest = hmac.digest(dataToSign);

        // 4. Base64 编码
        return Base64.getEncoder().encodeToString(digest);
    }

    /**
     * 参数排序并拼接
     *
     * @param params 请求参数
     * @return 排序后的参数字符串
     */
    public static String sortParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        // 过滤空值参数并按字典序排序
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null && StringUtils.hasText(entry.getValue().toString())) {
                keys.add(entry.getKey());
            }
        }

        Collections.sort(keys);

        // 拼接参数
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(params.get(key));
        }

        return sb.toString();
    }

    /**
     * 从请求中提取参数
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    public static Map<String, Object> extractParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 获取URL参数
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }

        return params;
    }

    /**
     * 验证时间戳（防重放攻击）
     *
     * @param timestamp 时间戳
     * @param tolerance 容忍时间差（秒）
     * @return 是否有效
     */
    public static boolean validateTimestamp(String timestamp, long tolerance) {
        if (!StringUtils.hasText(timestamp)) {
            return false;
        }

        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            return Math.abs(currentTime - requestTime) <= tolerance;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证签名
     *
     * @param params 请求参数
     * @param timestamp 时间戳
     * @param secret 密钥
     * @param receivedSignature 接收到的签名
     * @return 是否验证通过
     */
    public static boolean verifySignature(Map<String, Object> params, String timestamp,
                                        String secret, String receivedSignature) {
        if (!StringUtils.hasText(receivedSignature)) {
            return false;
        }

        String expectedSignature = generateSignature(params, timestamp, secret);
        return expectedSignature.equals(receivedSignature);
    }

    /**
     * 生成随机密钥
     *
     * @param length 密钥长度
     * @return 随机密钥
     */
    public static String generateSecretKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    /**
     * 生成当前时间戳（秒级）
     *
     * @return 时间戳字符串
     */
    public static String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }
}