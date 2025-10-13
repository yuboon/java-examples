package com.example.jwt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 密钥轮换调度器
 * 负责定期生成新密钥和清理过期密钥
 */
@Service
public class KeyRotationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeyRotationScheduler.class);

    @Autowired
    private DynamicKeyStore keyStore;

    @Value("${jwt.rotation-period-days:7}")
    private int rotationPeriodDays;

    @Value("${jwt.grace-period-days:14}")
    private int gracePeriodDays;

    /**
     * 应用启动后初始化密钥存储
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        logger.info("应用启动完成，开始初始化密钥轮换调度器...");
        keyStore.initialize();
        logger.info("密钥轮换调度器初始化完成");
        logger.info("配置信息 - 轮换周期: {} 天, 宽限期: {} 天", rotationPeriodDays, gracePeriodDays);
    }

    /**
     * 定时轮换密钥 - 每天凌晨2点检查是否需要轮换
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledKeyRotation() {
        logger.info("执行定时密钥轮换检查...");

        try {
            // 获取当前密钥统计信息
            var stats = keyStore.getStatistics();
            String currentKeyId = (String) stats.get("currentKeyId");

            // 检查当前密钥的创建时间
            var currentKey = keyStore.getCurrentKey();
            long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                currentKey.getCreatedAt(),
                java.time.LocalDateTime.now()
            );

            if (daysSinceCreation >= rotationPeriodDays) {
                logger.info("当前密钥 {} 已使用 {} 天，开始轮换", currentKeyId, daysSinceCreation);

                // 生成新密钥
                String newKeyId = keyStore.generateNewKeyPair();
                logger.info("密钥轮换完成: {} -> {}", currentKeyId, newKeyId);

                // 记录轮换事件
                logRotationEvent(currentKeyId, newKeyId, daysSinceCreation);
            } else {
                logger.debug("当前密钥 {} 仅使用 {} 天，暂不需要轮换", currentKeyId, daysSinceCreation);
            }

        } catch (Exception e) {
            logger.error("密钥轮换过程中发生错误", e);
        }
    }

    /**
     * 清理过期密钥 - 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledKeyCleanup() {
        logger.info("执行定时密钥清理...");

        try {
            List<String> removedKeys = keyStore.cleanupExpiredKeys(gracePeriodDays);

            if (!removedKeys.isEmpty()) {
                logger.info("清理完成，删除了 {} 个过期密钥: {}", removedKeys.size(), removedKeys);
            } else {
                logger.debug("没有需要清理的过期密钥");
            }

            // 输出清理后的统计信息
            var stats = keyStore.getStatistics();
            logger.info("清理后密钥存储状态: 总密钥数={}, 当前密钥={}",
                stats.get("totalKeys"), stats.get("currentKeyId"));

        } catch (Exception e) {
            logger.error("密钥清理过程中发生错误", e);
        }
    }

    /**
     * 密钥状态监控 - 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void monitorKeyStatus() {
        try {
            var stats = keyStore.getStatistics();
            int totalKeys = (Integer) stats.get("totalKeys");
            String currentKeyId = (String) stats.get("currentKeyId");

            // 检查密钥存储是否已初始化
            if (currentKeyId == null) {
                logger.warn("密钥存储尚未初始化，跳过监控");
                return;
            }

            // 检查密钥数量是否异常
            if (totalKeys > 5) {
                logger.warn("密钥数量较多: {}，建议检查清理策略", totalKeys);
            }

            // 检查当前密钥状态
            var currentKey = keyStore.getCurrentKey();
            if (currentKey == null) {
                logger.error("无法获取当前密钥，尝试重新初始化");
                keyStore.initialize();
                return;
            }

            long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                currentKey.getCreatedAt(),
                java.time.LocalDateTime.now()
            );

            if (daysSinceCreation >= rotationPeriodDays - 1) {
                logger.info("当前密钥 {} 即将到达轮换时间（已使用 {} 天）",
                    currentKeyId, daysSinceCreation);
            }

        } catch (Exception e) {
            logger.error("密钥状态监控发生错误", e);
        }
    }

    /**
     * 手动触发密钥轮换（用于测试或紧急情况）
     */
    public String forceKeyRotation() {
        logger.warn("手动触发密钥轮换...");

        var oldKey = keyStore.getCurrentKey();
        String oldKeyId = oldKey.getKeyId();

        String newKeyId = keyStore.generateNewKeyPair();

        logger.warn("手动密钥轮换完成: {} -> {}", oldKeyId, newKeyId);

        return newKeyId;
    }

    /**
     * 手动触发密钥清理（用于测试或紧急情况）
     */
    public List<String> forceKeyCleanup() {
        logger.warn("手动触发密钥清理...");

        List<String> removedKeys = keyStore.cleanupExpiredKeys(gracePeriodDays);

        logger.warn("手动密钥清理完成，删除了 {} 个密钥: {}", removedKeys.size(), removedKeys);

        return removedKeys;
    }

    /**
     * 记录密钥轮换事件
     */
    private void logRotationEvent(String oldKeyId, String newKeyId, long daysUsed) {
        logger.info("=== 密钥轮换事件 ===");
        logger.info("旧密钥: {} (使用天数: {})", oldKeyId, daysUsed);
        logger.info("新密钥: {}", newKeyId);
        logger.info("轮换时间: {}", java.time.LocalDateTime.now());
        logger.info("轮换周期: {} 天", rotationPeriodDays);
        logger.info("==================");
    }
}