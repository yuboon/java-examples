package com.example.netspeed.core;

import java.util.concurrent.locks.LockSupport;

/**
 * 令牌桶算法实现
 *
 * 核心原理：
 * 1. 桶容量：允许的突发流量上限
 * 2. 填充速率：长期平均传输速度
 * 3. 获取令牌：消耗对应数量的令牌，不足则等待
 */
public class TokenBucket {

    private final long capacity;              // 桶容量（字节）
    private final long initialRefillRate;     // 初始填充速率（字节/秒）
    private volatile long refillRate;         // 当前填充速率（字节/秒）
    private volatile long tokens;             // 当前令牌数（字节）
    private volatile long lastRefillTime;     // 上次填充时间（纳秒）

    // 统计信息
    private volatile long totalBytesConsumed;
    private volatile long totalWaitTimeNanos;
    private final long creationTime;

    public TokenBucket(long capacity, long refillRate) {
        this.capacity = capacity;
        this.initialRefillRate = refillRate;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillTime = System.nanoTime();
        this.totalBytesConsumed = 0;
        this.totalWaitTimeNanos = 0;
        this.creationTime = System.nanoTime();
    }

    /**
     * 获取令牌（阻塞等待）
     *
     * @param permits 需要的令牌数（字节数）
     */
    public synchronized void acquire(long permits) {
        if (permits <= 0) {
            return;
        }

        long waitTime = refillAndCalculateWait(permits);

        if (waitTime > 0) {
            sleepNanos(waitTime);
            totalWaitTimeNanos += waitTime;
            // 等待后再次填充并消费
            refill();
            tokens = Math.max(0, tokens - permits);
        } else {
            tokens -= permits;
        }

        totalBytesConsumed += permits;
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param permits 需要的令牌数
     * @return 是否成功获取
     */
    public synchronized boolean tryAcquire(long permits) {
        if (permits <= 0) {
            return true;
        }

        refill();

        if (tokens >= permits) {
            tokens -= permits;
            totalBytesConsumed += permits;
            return true;
        }

        return false;
    }

    /**
     * 填充令牌并计算需要等待的时间
     */
    private long refillAndCalculateWait(long permits) {
        refill();

        if (tokens >= permits) {
            return 0;
        }

        // 令牌不足，计算需要等待的时间
        long deficit = permits - tokens;
        return (deficit * 1_000_000_000L) / refillRate;
    }

    /**
     * 填充令牌（核心逻辑）
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTime;

        if (elapsedNanos <= 0) {
            return;
        }

        // 根据时间差计算补充的令牌数
        long newTokens = (elapsedNanos * refillRate) / 1_000_000_000L;

        if (newTokens > 0) {
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillTime = now;
        }
    }

    /**
     * 精确纳秒级等待
     */
    private void sleepNanos(long nanos) {
        if (nanos <= 0) {
            return;
        }

        long end = System.nanoTime() + nanos;
        while (System.nanoTime() < end) {
            LockSupport.parkNanos(Math.max(1000, end - System.nanoTime()));
        }
    }

    /**
     * 获取当前可用令牌数
     */
    public long getAvailableTokens() {
        refill();
        return tokens;
    }

    /**
     * 动态调整填充速率
     */
    public synchronized void setRefillRate(long newRate) {
        this.refillRate = newRate;
        refill();
    }

    /**
     * 重置令牌桶
     */
    public synchronized void reset() {
        this.tokens = capacity;
        this.refillRate = initialRefillRate;
        this.lastRefillTime = System.nanoTime();
    }

    /**
     * 获取实际传输速率
     */
    public double getActualRate() {
        long elapsedNanos = System.nanoTime() - creationTime;
        if (elapsedNanos <= 0) {
            return 0;
        }
        long elapsedSeconds = elapsedNanos / 1_000_000_000L;
        return elapsedSeconds > 0 ? (double) totalBytesConsumed / elapsedSeconds : 0;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getRefillRate() {
        return refillRate;
    }

    public long getTotalBytesConsumed() {
        return totalBytesConsumed;
    }

    public long getTotalWaitTimeNanos() {
        return totalWaitTimeNanos;
    }

    public double getUtilization() {
        return capacity > 0 ? (double) tokens / capacity : 0;
    }
}
