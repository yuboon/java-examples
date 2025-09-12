package com.example.dashboard.model;

import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Data
public class MethodMetrics {
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);
    private final LongAdder totalTime = new LongAdder();
    private volatile long maxTime = 0;
    private volatile long minTime = Long.MAX_VALUE;
    private final String methodName;
    private volatile long lastAccessTime = System.currentTimeMillis();

    public MethodMetrics(String methodName) {
        this.methodName = methodName;
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
        lastAccessTime = System.currentTimeMillis();
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
}