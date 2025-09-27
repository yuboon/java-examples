package com.example.exceptiongroup.cache;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LRU指纹缓存系统 - 单例模式
 * 确保Logback Appender和Spring Controller使用同一个缓存实例
 */
@Component
public class ErrorFingerprintCache {

    private static ErrorFingerprintCache INSTANCE;

    private static final int DEFAULT_CAPACITY = 1000;
    private static final int DEFAULT_LOG_THRESHOLD = 10;
    private static final int MAX_RECENT_TRACES = 5;

    private final int capacity;
    private final int logThreshold;
    private final Map<String, ErrorFingerprint> cache;

    public ErrorFingerprintCache() {
        this.capacity = DEFAULT_CAPACITY;
        this.logThreshold = DEFAULT_LOG_THRESHOLD;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, ErrorFingerprint>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ErrorFingerprint> eldest) {
                return size() > capacity;
            }
        });
        // 设置单例实例
        INSTANCE = this;
    }

    /**
     * 获取单例实例（供Logback Appender使用）
     */
    public static ErrorFingerprintCache getInstance() {
        if (INSTANCE == null) {
            synchronized (ErrorFingerprintCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ErrorFingerprintCache();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 检查是否应该记录日志
     */
    public boolean shouldLog(String fingerprint, String traceId) {
        return shouldLog(fingerprint, traceId, null, null);
    }

    /**
     * 检查是否应该记录日志（重载方法，支持异常信息）
     */
    public boolean shouldLog(String fingerprint, String traceId, String exceptionType, String stackTrace) {
        ErrorFingerprint errorInfo = cache.computeIfAbsent(fingerprint,
            k -> new ErrorFingerprint(fingerprint, traceId, exceptionType, stackTrace));

        long count = errorInfo.incrementAndGet();
        errorInfo.setLastOccurrence(LocalDateTime.now());
        errorInfo.addRecentTraceId(traceId);

        // 首次出现或达到阈值时记录日志
        return count == 1 || count % logThreshold == 0;
    }

    /**
     * 获取所有错误指纹统计
     */
    public List<ErrorFingerprint> getAllFingerprints() {
        synchronized (cache) {
            return new ArrayList<>(cache.values());
        }
    }

    /**
     * 根据指纹获取详细信息
     */
    public ErrorFingerprint getFingerprint(String fingerprint) {
        return cache.get(fingerprint);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 错误指纹信息类
     */
    public static class ErrorFingerprint {
        private final String fingerprint;
        private final LocalDateTime firstOccurrence;
        private volatile LocalDateTime lastOccurrence;
        private final AtomicLong count = new AtomicLong(0);
        private final String sampleTraceId;
        private final String exceptionType;
        private final String stackTrace;
        private final Queue<String> recentTraceIds = new ArrayDeque<>(MAX_RECENT_TRACES);

        public ErrorFingerprint(String fingerprint, String sampleTraceId) {
            this(fingerprint, sampleTraceId, null, null);
        }

        public ErrorFingerprint(String fingerprint, String sampleTraceId, String exceptionType, String stackTrace) {
            this.fingerprint = fingerprint;
            this.sampleTraceId = sampleTraceId;
            this.exceptionType = exceptionType;
            this.stackTrace = stackTrace;
            this.firstOccurrence = LocalDateTime.now();
            this.lastOccurrence = this.firstOccurrence;
        }

        public long incrementAndGet() {
            return count.incrementAndGet();
        }

        public synchronized void addRecentTraceId(String traceId) {
            if (recentTraceIds.size() >= MAX_RECENT_TRACES) {
                recentTraceIds.poll();
            }
            recentTraceIds.offer(traceId);
        }

        // Getters
        public String getFingerprint() { return fingerprint; }
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
        public long getCount() { return count.get(); }
        public String getSampleTraceId() { return sampleTraceId; }
        public String getExceptionType() { return exceptionType; }
        public String getStackTrace() { return stackTrace; }
        public synchronized List<String> getRecentTraceIds() { return new ArrayList<>(recentTraceIds); }
    }
}