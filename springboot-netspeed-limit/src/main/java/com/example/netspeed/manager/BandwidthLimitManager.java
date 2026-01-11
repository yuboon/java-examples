package com.example.netspeed.manager;

import com.example.netspeed.annotation.LimitType;
import com.example.netspeed.core.TokenBucket;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 带宽限速管理器
 *
 * 管理多维度的令牌桶：
 * - GLOBAL: 全局共享一个令牌桶
 * - API: 每个接口路径一个令牌桶
 * - USER: 每个用户ID一个令牌桶
 * - IP: 每个IP地址一个令牌桶
 */
@Slf4j
public class BandwidthLimitManager {

    // 全局限速桶
    private TokenBucket globalBucket;

    // API维度限速桶 (path -> TokenBucket)
    private final ConcurrentHashMap<String, TokenBucket> apiBuckets = new ConcurrentHashMap<>();

    // 用户维度限速桶 (userId -> TokenBucket)
    private final ConcurrentHashMap<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();

    // IP维度限速桶 (ip -> TokenBucket)
    private final ConcurrentHashMap<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    // 定时清理服务
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "bandwidth-limit-cleanup");
        thread.setDaemon(true);
        return thread;
    });

    // 最后使用时间记录
    private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    // 空闲超时时间（毫秒）
    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5分钟

    public BandwidthLimitManager() {
        // 启动定时清理任务
        startCleanupTask();
    }

    /**
     * 获取或创建令牌桶
     */
    public TokenBucket getBucket(LimitType type, String key, long capacity, long refillRate) {
        return switch (type) {
            case GLOBAL -> getGlobalBucket(capacity, refillRate);
            case API -> getOrCreateBucket(apiBuckets, key, capacity, refillRate);
            case USER -> getOrCreateBucket(userBuckets, key, capacity, refillRate);
            case IP -> getOrCreateBucket(ipBuckets, key, capacity, refillRate);
        };
    }

    /**
     * 获取全局限速桶
     */
    private synchronized TokenBucket getGlobalBucket(long capacity, long refillRate) {
        if (globalBucket == null) {
            globalBucket = new TokenBucket(capacity, refillRate);
            log.info("Created global bandwidth limit bucket: capacity={}, rate={}/s",
                capacity, formatBytes(refillRate));
        } else if (globalBucket.getRefillRate() != refillRate) {
            // 动态调整速率
            globalBucket.setRefillRate(refillRate);
            log.info("Updated global bandwidth limit rate: {}/s", formatBytes(refillRate));
        }
        return globalBucket;
    }

    /**
     * 获取或创建指定维度的令牌桶
     */
    private TokenBucket getOrCreateBucket(ConcurrentHashMap<String, TokenBucket> buckets,
                                          String key,
                                          long capacity,
                                          long refillRate) {
        return buckets.compute(key, (k, existing) -> {
            if (existing == null) {
                log.debug("Created new bandwidth limit bucket for {}: capacity={}, rate={}/s",
                    k, capacity, formatBytes(refillRate));
                return new TokenBucket(capacity, refillRate);
            }

            // 更新最后访问时间
            lastAccessTime.put(key, System.currentTimeMillis());

            // 动态调整速率
            if (existing.getRefillRate() != refillRate) {
                existing.setRefillRate(refillRate);
                log.debug("Updated bandwidth limit rate for {}: {}/s", k, formatBytes(refillRate));
            }

            return existing;
        });
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupIdleBuckets();
            } catch (Exception e) {
                log.error("Error during cleanup", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 清理空闲的令牌桶
     */
    private void cleanupIdleBuckets() {
        long now = System.currentTimeMillis();

        // 清理 API 维度
        cleanupMap(apiBuckets, now, "API");
        // 清理用户维度
        cleanupMap(userBuckets, now, "USER");
        // 清理 IP 维度
        cleanupMap(ipBuckets, now, "IP");

        // 清理访问时间记录
        lastAccessTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > IDLE_TIMEOUT_MS) {
                return true;
            }
            return false;
        });
    }

    private void cleanupMap(ConcurrentHashMap<String, TokenBucket> buckets, long now, String type) {
        buckets.keySet().removeIf(key -> {
            Long lastAccess = lastAccessTime.get(key);
            if (lastAccess == null || now - lastAccess > IDLE_TIMEOUT_MS) {
                log.debug("Removed idle {} bandwidth bucket: {}", type, key);
                lastAccessTime.remove(key);
                return true;
            }
            return false;
        });
    }

    /**
     * 获取统计信息
     */
    public BandwidthLimitStats getStats() {
        if (globalBucket != null) {
            return new BandwidthLimitStats(
                globalBucket.getCapacity(),
                globalBucket.getRefillRate(),
                globalBucket.getAvailableTokens(),
                globalBucket.getTotalBytesConsumed(),
                globalBucket.getActualRate(),
                globalBucket.getTotalWaitTimeNanos(),
                globalBucket.getUtilization(),
                apiBuckets.size(),
                userBuckets.size(),
                ipBuckets.size()
            );
        }
        return new BandwidthLimitStats(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    /**
     * 重置全局限速桶
     */
    public void resetGlobalBucket() {
        if (globalBucket != null) {
            globalBucket.reset();
            log.info("Reset global bandwidth limit bucket");
        }
    }

    /**
     * 清除所有维度的限速桶（除了全局）
     */
    public void clearAllBuckets() {
        apiBuckets.clear();
        userBuckets.clear();
        ipBuckets.clear();
        lastAccessTime.clear();
        log.info("Cleared all bandwidth limit buckets");
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 统计信息
     */
    public record BandwidthLimitStats(
        long globalCapacity,           // 全局桶容量
        long globalRefillRate,         // 全局填充速率（字节/秒）
        long globalAvailableTokens,    // 全局可用令牌
        long globalBytesConsumed,      // 全局已消耗字节
        double globalActualRate,       // 全局实际传输速率（字节/秒）
        long globalWaitTimeNanos,      // 全局等待时间（纳秒）
        double globalUtilization,      // 全局利用率（0-1）
        int apiBucketCount,            // API限速桶数量
        int userBucketCount,           // 用户限速桶数量
        int ipBucketCount              // IP限速桶数量
    ) {}
}
