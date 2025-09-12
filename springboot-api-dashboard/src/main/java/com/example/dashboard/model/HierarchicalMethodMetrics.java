package com.example.dashboard.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Data
public class HierarchicalMethodMetrics {
    
    // 基础统计信息（兼容旧版本）
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);
    private final LongAdder totalTime = new LongAdder();
    private volatile long maxTime = 0;
    private volatile long minTime = Long.MAX_VALUE;
    private final String methodName;
    private volatile long lastAccessTime = System.currentTimeMillis();

    // 分级时间桶
    private final ConcurrentHashMap<Long, TimeBucket> secondBuckets = new ConcurrentHashMap<>();  // 最近5分钟，秒级
    private final ConcurrentHashMap<Long, TimeBucket> minuteBuckets = new ConcurrentHashMap<>();  // 最近1小时，分钟级
    private final ConcurrentHashMap<Long, TimeBucket> hourBuckets = new ConcurrentHashMap<>();    // 最近24小时，小时级
    private final ConcurrentHashMap<Long, TimeBucket> dayBuckets = new ConcurrentHashMap<>();     // 最近7天，天级

    // 时间常量
    private static final long SECOND_MILLIS = 1000;
    private static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;

    // 保留时长
    private static final long KEEP_SECONDS = 5 * 60;      // 5分钟
    private static final long KEEP_MINUTES = 60;          // 1小时
    private static final long KEEP_HOURS = 24;            // 24小时
    private static final long KEEP_DAYS = 7;              // 7天

    public HierarchicalMethodMetrics(String methodName) {
        this.methodName = methodName;
    }

    public synchronized void record(long duration, boolean success) {
        long currentTime = System.currentTimeMillis();
        
        // 更新基础统计
        totalCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failCount.incrementAndGet();
        }
        totalTime.add(duration);
        maxTime = Math.max(maxTime, duration);
        minTime = Math.min(minTime, duration);
        lastAccessTime = currentTime;

        // 分级记录到不同时间桶
        recordToTimeBuckets(currentTime, duration, success);
        
        // 清理过期桶
        cleanupExpiredBuckets(currentTime);
    }

    private void recordToTimeBuckets(long currentTime, long duration, boolean success) {
        // 秒级桶
        long secondKey = currentTime / SECOND_MILLIS;
        secondBuckets.computeIfAbsent(secondKey, k -> new TimeBucket(k * SECOND_MILLIS))
                     .record(duration, success);

        // 分钟级桶
        long minuteKey = currentTime / MINUTE_MILLIS;
        minuteBuckets.computeIfAbsent(minuteKey, k -> new TimeBucket(k * MINUTE_MILLIS))
                     .record(duration, success);

        // 小时级桶
        long hourKey = currentTime / HOUR_MILLIS;
        hourBuckets.computeIfAbsent(hourKey, k -> new TimeBucket(k * HOUR_MILLIS))
                   .record(duration, success);

        // 天级桶
        long dayKey = currentTime / DAY_MILLIS;
        dayBuckets.computeIfAbsent(dayKey, k -> new TimeBucket(k * DAY_MILLIS))
                  .record(duration, success);
    }

    private void cleanupExpiredBuckets(long currentTime) {
        // 清理过期的秒级桶（保留5分钟）
        long expiredSecond = (currentTime / SECOND_MILLIS) - KEEP_SECONDS;
        secondBuckets.entrySet().removeIf(entry -> entry.getKey() < expiredSecond);

        // 清理过期的分钟级桶（保留1小时）
        long expiredMinute = (currentTime / MINUTE_MILLIS) - KEEP_MINUTES;
        minuteBuckets.entrySet().removeIf(entry -> entry.getKey() < expiredMinute);

        // 清理过期的小时级桶（保留24小时）
        long expiredHour = (currentTime / HOUR_MILLIS) - KEEP_HOURS;
        hourBuckets.entrySet().removeIf(entry -> entry.getKey() < expiredHour);

        // 清理过期的天级桶（保留7天）
        long expiredDay = (currentTime / DAY_MILLIS) - KEEP_DAYS;
        dayBuckets.entrySet().removeIf(entry -> entry.getKey() < expiredDay);
    }

    public TimeRangeMetrics queryTimeRange(long startTime, long endTime) {
        List<TimeBucket.TimeBucketSnapshot> buckets = selectBucketsForTimeRange(startTime, endTime);
        return aggregateSnapshots(buckets, startTime, endTime);
    }

    private List<TimeBucket.TimeBucketSnapshot> selectBucketsForTimeRange(long startTime, long endTime) {
        long duration = endTime - startTime;
        List<TimeBucket.TimeBucketSnapshot> result = new ArrayList<>();

        if (duration <= 5 * MINUTE_MILLIS) {
            // 5分钟内，使用秒级桶
            long startSecond = startTime / SECOND_MILLIS;
            long endSecond = endTime / SECOND_MILLIS;
            for (long sec = startSecond; sec <= endSecond; sec++) {
                TimeBucket bucket = secondBuckets.get(sec);
                if (bucket != null && !bucket.isEmpty()) {
                    result.add(bucket.toSnapshot());
                }
            }
        } else if (duration <= HOUR_MILLIS) {
            // 1小时内，使用分钟级桶
            long startMinute = startTime / MINUTE_MILLIS;
            long endMinute = endTime / MINUTE_MILLIS;
            for (long min = startMinute; min <= endMinute; min++) {
                TimeBucket bucket = minuteBuckets.get(min);
                if (bucket != null && !bucket.isEmpty()) {
                    result.add(bucket.toSnapshot());
                }
            }
        } else if (duration <= DAY_MILLIS) {
            // 1天内，使用小时级桶
            long startHour = startTime / HOUR_MILLIS;
            long endHour = endTime / HOUR_MILLIS;
            for (long hour = startHour; hour <= endHour; hour++) {
                TimeBucket bucket = hourBuckets.get(hour);
                if (bucket != null && !bucket.isEmpty()) {
                    result.add(bucket.toSnapshot());
                }
            }
        } else {
            // 超过1天，使用天级桶
            long startDay = startTime / DAY_MILLIS;
            long endDay = endTime / DAY_MILLIS;
            for (long day = startDay; day <= endDay; day++) {
                TimeBucket bucket = dayBuckets.get(day);
                if (bucket != null && !bucket.isEmpty()) {
                    result.add(bucket.toSnapshot());
                }
            }
        }

        return result;
    }

    private TimeRangeMetrics aggregateSnapshots(List<TimeBucket.TimeBucketSnapshot> snapshots, 
                                               long startTime, long endTime) {
        if (snapshots.isEmpty()) {
            return new TimeRangeMetrics(methodName, startTime, endTime, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        long totalCount = 0;
        long successCount = 0; 
        long failCount = 0;
        long totalTime = 0;
        long maxTime = 0;
        long minTime = Long.MAX_VALUE;

        for (TimeBucket.TimeBucketSnapshot snapshot : snapshots) {
            totalCount += snapshot.getTotalCount();
            successCount += snapshot.getSuccessCount();
            failCount += snapshot.getFailCount();
            totalTime += snapshot.getTotalTime();
            maxTime = Math.max(maxTime, snapshot.getMaxTime());
            minTime = Math.min(minTime, snapshot.getMinTime());
        }

        double avgTime = totalCount > 0 ? (double) totalTime / totalCount : 0;
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;

        return new TimeRangeMetrics(
            methodName, startTime, endTime,
            totalCount, successCount, failCount,
            totalTime, maxTime, minTime == Long.MAX_VALUE ? 0 : minTime,
            avgTime, successRate
        );
    }

    // 兼容旧版本的方法
    public double getAvgTime() {
        long total = totalCount.get();
        return total == 0 ? 0.0 : (double) totalTime.sum() / total;
    }

    public double getSuccessRate() {
        long total = totalCount.get();
        return total == 0 ? 0.0 : (double) successCount.get() / total * 100;
    }

    public long getMinTime() {
        return minTime == Long.MAX_VALUE ? 0 : minTime;
    }

    @Data
    public static class TimeRangeMetrics {
        private final String methodName;
        private final long startTime;
        private final long endTime;
        private final long totalCount;
        private final long successCount;
        private final long failCount;
        private final long totalTime;
        private final long maxTime;
        private final long minTime;
        private final double avgTime;
        private final double successRate;
    }
}