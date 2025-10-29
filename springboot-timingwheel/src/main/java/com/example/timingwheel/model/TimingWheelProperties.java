package com.example.timingwheel.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 时间轮配置
 */
@Data
@ConfigurationProperties(prefix = "timingwheel.config")
public class TimingWheelProperties {
    /**
     * 槽位数量，默认为512
     */
    private int slotSize = 512;

    /**
     * 每个槽位的时间间隔（毫秒），默认为100ms
     */
    private long tickDuration = 100;

    /**
     * 工作线程数量
     */
    private int workerThreads = 4;

    /**
     * 是否启用多层时间轮
     */
    private boolean enableMultiWheel = true;

    /**
     * 最大时间轮层级
     */
    private int maxWheelLevels = 3;

    /**
     * 是否启用指标监控
     */
    private boolean enableMetrics = true;

    /**
     * 任务队列最大容量
     */
    private int maxQueueSize = 10000;

    /**
     * 是否启用任务持久化
     */
    private boolean enablePersistence = false;

    /**
     * 任务执行超时时间（毫秒）
     */
    private long taskTimeout = 30000;
}