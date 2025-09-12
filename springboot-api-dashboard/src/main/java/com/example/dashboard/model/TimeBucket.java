package com.example.dashboard.model;

import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Data
public class TimeBucket {
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);
    private final LongAdder totalTime = new LongAdder();
    private volatile long maxTime = 0;
    private volatile long minTime = Long.MAX_VALUE;
    private final long bucketStartTime;
    private volatile long lastUpdateTime;

    public TimeBucket(long bucketStartTime) {
        this.bucketStartTime = bucketStartTime;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public synchronized void record(long duration, boolean success) {
        totalCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failCount.incrementAndGet();
        }
        
        totalTime.add(duration);
        maxTime = Math.max(maxTime, duration);
        minTime = Math.min(minTime, duration);
        lastUpdateTime = System.currentTimeMillis();
    }

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

    public boolean isEmpty() {
        return totalCount.get() == 0;
    }

    public TimeBucketSnapshot toSnapshot() {
        return new TimeBucketSnapshot(
            bucketStartTime,
            totalCount.get(),
            successCount.get(),
            failCount.get(),
            totalTime.sum(),
            maxTime,
            getMinTime(),
            getAvgTime(),
            getSuccessRate()
        );
    }

    @Data
    public static class TimeBucketSnapshot {
        private final long bucketStartTime;
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