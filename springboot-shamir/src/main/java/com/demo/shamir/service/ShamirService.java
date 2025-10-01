package com.demo.shamir.service;

import com.demo.shamir.dto.*;
import com.demo.shamir.util.ShamirUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Shamir 密钥共享服务
 * 注意：此处使用 Map 存储仅用于演示，实际生产环境应存储到数据库或其他持久化存储
 */
@Service
public class ShamirService {

    /**
     * 存储会话信息（演示用，生产环境应使用数据库）
     * key: sessionId, value: 会话元数据
     */
    private final Map<String, SessionMetadata> sessionStore = new ConcurrentHashMap<>();

    /**
     * 会话元数据
     */
    private static class SessionMetadata {
        String sessionId;
        int totalShares;
        int threshold;
        long createTime;

        SessionMetadata(String sessionId, int totalShares, int threshold) {
            this.sessionId = sessionId;
            this.totalShares = totalShares;
            this.threshold = threshold;
            this.createTime = System.currentTimeMillis();
        }
    }

    /**
     * 拆分密钥
     */
    public SplitResponse split(SplitRequest request) {
        // 参数校验
        if (request.getSecret() == null || request.getSecret().isEmpty()) {
            throw new IllegalArgumentException("Secret cannot be empty");
        }
        if (request.getTotalShares() == null || request.getTotalShares() < 2) {
            throw new IllegalArgumentException("Total shares must be at least 2");
        }
        if (request.getThreshold() == null || request.getThreshold() < 2) {
            throw new IllegalArgumentException("Threshold must be at least 2");
        }
        if (request.getThreshold() > request.getTotalShares()) {
            throw new IllegalArgumentException("Threshold cannot exceed total shares");
        }

        // 将密钥转换为字节数组
        byte[] secretBytes = request.getSecret().getBytes(StandardCharsets.UTF_8);

        // 调用 Shamir 算法拆分
        List<ShamirUtils.Share> shares = ShamirUtils.split(
                secretBytes,
                request.getTotalShares(),
                request.getThreshold()
        );

        // 编码份额为字符串
        List<String> encodedShares = shares.stream()
                .map(ShamirUtils.Share::encode)
                .collect(Collectors.toList());

        // 生成会话 ID
        String sessionId = UUID.randomUUID().toString();

        // 存储会话元数据（演示用）
        sessionStore.put(sessionId, new SessionMetadata(
                sessionId,
                request.getTotalShares(),
                request.getThreshold()
        ));

        return new SplitResponse(
                sessionId,
                encodedShares,
                String.format("密钥已拆分为 %d 份，任意 %d 份可恢复原始密钥",
                        request.getTotalShares(), request.getThreshold())
        );
    }

    /**
     * 恢复密钥
     */
    public CombineResponse combine(CombineRequest request) {
        try {
            // 参数校验
            if (request.getShares() == null || request.getShares().isEmpty()) {
                return new CombineResponse(null, "密钥份额列表不能为空", false);
            }

            // 解码份额
            List<ShamirUtils.Share> shares = request.getShares().stream()
                    .map(ShamirUtils.Share::decode)
                    .collect(Collectors.toList());

            // 调用 Shamir 算法恢复
            byte[] secretBytes = ShamirUtils.combine(shares);

            // 转换为字符串（处理可能的多余字节）
            String secret = new String(secretBytes, StandardCharsets.UTF_8).trim();

            // 移除可能的前导零字节
            if (secret.charAt(0) == '\0') {
                secret = secret.substring(1);
            }

            return new CombineResponse(
                    secret,
                    String.format("成功使用 %d 个份额恢复密钥", shares.size()),
                    true
            );

        } catch (Exception e) {
            return new CombineResponse(
                    null,
                    "恢复失败：" + e.getMessage(),
                    false
            );
        }
    }

    /**
     * 获取所有会话（演示用）
     */
    public Map<String, SessionMetadata> getAllSessions() {
        return new HashMap<>(sessionStore);
    }

    /**
     * 清理过期会话（演示用，生产环境应使用定时任务）
     */
    public void cleanExpiredSessions(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        sessionStore.entrySet().removeIf(entry ->
                now - entry.getValue().createTime > maxAgeMillis
        );
    }
}