package com.example.timingwheel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 时间轮任务包装类
 */
@Data
@Slf4j
public class TimerTaskWrapper {
    private final String taskId;
    private final TimerTask task;
    private final long delayMs;
    private final long createTime;
    private final long expireTime;
    private final AtomicInteger rounds;

    private volatile TaskStatus status;
    private volatile LocalDateTime executeTime;
    private volatile String errorMessage;

    public enum TaskStatus {
        PENDING,     // 等待执行
        RUNNING,     // 正在执行
        COMPLETED,   // 执行完成
        FAILED,      // 执行失败
        CANCELLED    // 已取消
    }

    public TimerTaskWrapper(TimerTask task, long delayMs) {
        this.taskId = UUID.randomUUID().toString();
        this.task = task;
        this.delayMs = delayMs;
        this.createTime = System.currentTimeMillis();
        this.expireTime = this.createTime + delayMs;
        this.rounds = new AtomicInteger(0);
        this.status = TaskStatus.PENDING;
    }

    public void markAsRunning() {
        this.status = TaskStatus.RUNNING;
        this.executeTime = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = TaskStatus.COMPLETED;
    }

    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        log.error("Task {} failed: {}", taskId, errorMessage);
    }

    public void markAsCancelled() {
        this.status = TaskStatus.CANCELLED;
    }

    public boolean isExpired() {
        return rounds.get() <= 0;
    }

    public void decrementRounds() {
        rounds.decrementAndGet();
    }

    public void setRounds(int rounds) {
        this.rounds.set(rounds);
    }

    @JsonProperty("rounds")
    public int getRounds() {
        return rounds.get();
    }

    public long getRemainingDelay() {
        long currentTime = System.currentTimeMillis();
        return Math.max(0, expireTime - currentTime);
    }

    public double getProgressPercentage() {
        long totalDuration = delayMs;
        long elapsed = System.currentTimeMillis() - createTime;
        return Math.min(100.0, (elapsed * 100.0) / totalDuration);
    }
}