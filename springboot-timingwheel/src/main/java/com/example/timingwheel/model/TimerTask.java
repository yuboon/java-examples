package com.example.timingwheel.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 定时任务接口
 */
public interface TimerTask {
    /**
     * 任务执行方法
     */
    void run();

    /**
     * 任务描述
     */
    String getDescription();

    /**
     * 任务ID
     */
    default String getTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 任务创建时间
     */
    default LocalDateTime getCreateTime() {
        return LocalDateTime.now();
    }
}