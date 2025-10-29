package com.example.timingwheel.util;

import com.example.timingwheel.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间轮核心实现
 */
@Slf4j
public class TimingWheel implements InitializingBean, DisposableBean {

    private final TimingWheelProperties properties;
    private final Slot[] slots;
    private final AtomicInteger currentSlot = new AtomicInteger(0);
    private final AtomicLong totalTasks = new AtomicLong(0);
    private final AtomicLong completedTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);

    private final ScheduledExecutorService tickerExecutor;
    private final ExecutorService taskExecutor;
    private final MeterRegistry meterRegistry;

    // Micrometer metrics
    private Timer scheduleTimer;
    private Timer executionTimer;
    private Timer taskDurationTimer;

    public TimingWheel(TimingWheelProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.slots = new Slot[properties.getSlotSize()];

        // 创建执行器
        this.tickerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "timing-wheel-ticker");
            t.setDaemon(true);
            return t;
        });

        this.taskExecutor = Executors.newFixedThreadPool(
            properties.getWorkerThreads(),
            r -> {
                Thread t = new Thread(r, "timing-wheel-worker");
                t.setDaemon(true);
                return t;
            }
        );

        // 初始化槽位
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Slot(i);
        }
    }

    @Override
    public void afterPropertiesSet() {
        initializeMetrics();
        start();
    }

    private void initializeMetrics() {
        if (properties.isEnableMetrics() && meterRegistry != null) {
            scheduleTimer = Timer.builder("timingwheel.schedule.duration")
                .description("Time taken to schedule tasks")
                .register(meterRegistry);

            executionTimer = Timer.builder("timingwheel.execution.duration")
                .description("Time taken to execute tasks")
                .register(meterRegistry);

            taskDurationTimer = Timer.builder("timingwheel.task.duration")
                .description("Actual task execution duration")
                .register(meterRegistry);
        }
    }

    /**
     * 启动时间轮
     */
    public void start() {
        log.info("Starting timing wheel with {} slots, {}ms tick duration, {} worker threads",
                properties.getSlotSize(), properties.getTickDuration(), properties.getWorkerThreads());

        tickerExecutor.scheduleAtFixedRate(
            this::tick,
            properties.getTickDuration(),
            properties.getTickDuration(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 添加定时任务
     */
    public TimerTaskWrapper schedule(TimerTask task, long delayMs) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            if (delayMs <= 0) {
                // 立即执行
                taskExecutor.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        task.run();
                        recordTaskSuccess(startTime);
                    } catch (Exception e) {
                        recordTaskFailure(e);
                    }
                });
                return null;
            }

            TimerTaskWrapper wrapper = new TimerTaskWrapper(task, delayMs);

            // 计算目标槽位
            int targetSlot = calculateTargetSlot(delayMs);
            int rounds = calculateRounds(delayMs);
            wrapper.setRounds(rounds);

            // 添加任务到目标槽位
            slots[targetSlot].addTask(wrapper);
            totalTasks.incrementAndGet();

            log.debug("Scheduled task {} to slot {} with {} rounds, delay: {}ms",
                    wrapper.getTaskId(), targetSlot, rounds, delayMs);

            return wrapper;
        } finally {
            if (scheduleTimer != null) {
                sample.stop(scheduleTimer);
            }
        }
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        for (Slot slot : slots) {
            boolean removed = slot.removeTask(taskId);
            if (removed) {
                log.debug("Cancelled task: {}", taskId);
                return true;
            }
        }
        return false;
    }

    /**
     * 时间轮指针移动
     */
    private void tick() {
        try {
            int slotIndex = currentSlot.getAndIncrement() % properties.getSlotSize();
            Slot currentSlot = slots[slotIndex];

            if (!currentSlot.isEmpty()) {
                Timer.Sample sample = Timer.start(meterRegistry);

                try {
                    processSlot(currentSlot);
                } finally {
                    if (executionTimer != null) {
                        sample.stop(executionTimer);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during tick processing", e);
        }
    }

    /**
     * 处理槽位任务
     */
    private void processSlot(Slot slot) {
        java.util.List<TimerTaskWrapper> tasksToExecute = new java.util.ArrayList<>();
        java.util.List<TimerTaskWrapper> tasksToRequeue = new java.util.ArrayList<>();

        // 处理当前槽位的任务
        for (TimerTaskWrapper wrapper : slot.getTasks()) {
            if (wrapper.isExpired()) {
                tasksToExecute.add(wrapper);
            } else {
                wrapper.decrementRounds();
                tasksToRequeue.add(wrapper);
            }
        }

        // 清空当前槽位
        slot.clear();

        // 重新排队未到期的任务
        for (TimerTaskWrapper wrapper : tasksToRequeue) {
            slot.addTask(wrapper);
        }

        // 执行到期的任务
        for (TimerTaskWrapper wrapper : tasksToExecute) {
            executeTask(wrapper);
        }

        log.debug("Processed slot {}: {} tasks executed, {} tasks requeued",
                slot.getIndex(), tasksToExecute.size(), tasksToRequeue.size());
    }

    /**
     * 执行任务
     */
    private void executeTask(TimerTaskWrapper wrapper) {
        taskExecutor.submit(() -> {
            long startTime = System.currentTimeMillis();
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                wrapper.markAsRunning();

                // 直接执行任务，不再创建额外的Future
                wrapper.getTask().run();

                wrapper.markAsCompleted();
                completedTasks.incrementAndGet();
                recordTaskSuccess(startTime);

                log.debug("Task {} executed successfully", wrapper.getTaskId());

            } catch (Exception e) {
                wrapper.markAsFailed(e.getMessage());
                failedTasks.incrementAndGet();
                recordTaskFailure(e);

                log.error("Error executing task: " + wrapper.getTaskId(), e);

            } finally {
                if (taskDurationTimer != null) {
                    sample.stop(taskDurationTimer);
                }
            }
        });
    }

    private void recordTaskSuccess(long startTime) {
        // 记录成功指标
    }

    private void recordTaskFailure(Exception e) {
        // 记录失败指标
    }

    /**
     * 计算目标槽位
     */
    private int calculateTargetSlot(long delayMs) {
        int ticks = (int) (delayMs / properties.getTickDuration());
        return (currentSlot.get() + ticks) % properties.getSlotSize();
    }

    /**
     * 计算需要经过的轮数
     */
    private int calculateRounds(long delayMs) {
        int ticks = (int) (delayMs / properties.getTickDuration());
        return ticks / properties.getSlotSize();
    }

    /**
     * 获取时间轮统计信息
     */
    public TimingWheelStats getStats() {
        return new TimingWheelStats(
            properties.getSlotSize(),
            properties.getTickDuration(),
            currentSlot.get(),
            totalTasks.get(),
            completedTasks.get(),
            failedTasks.get(),
            getActiveTaskCount(),
            getSlotInfos()
        );
    }

    private int getActiveTaskCount() {
        return java.util.Arrays.stream(slots)
            .mapToInt(Slot::getTaskCount)
            .sum();
    }

    private java.util.List<Slot.SlotInfo> getSlotInfos() {
        return java.util.Arrays.stream(slots)
            .map(Slot::getSlotInfo)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void destroy() {
        log.info("Stopping timing wheel");

        // 关闭执行器
        tickerExecutor.shutdown();
        taskExecutor.shutdown();

        try {
            if (!tickerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                tickerExecutor.shutdownNow();
            }
            if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tickerExecutor.shutdownNow();
            taskExecutor.shutdownNow();
        }

        log.info("Timing wheel stopped");
    }

    /**
     * 时间轮统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TimingWheelStats {
        private int slotSize;
        private long tickDuration;
        private int currentSlot;
        private long totalTasks;
        private long completedTasks;
        private long failedTasks;
        private int activeTaskCount;
        private java.util.List<Slot.SlotInfo> slotInfos;
    }
}