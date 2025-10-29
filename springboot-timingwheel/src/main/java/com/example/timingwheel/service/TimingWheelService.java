package com.example.timingwheel.service;

import com.example.timingwheel.model.TimerTask;
import com.example.timingwheel.model.TimerTaskWrapper;
import com.example.timingwheel.util.TimingWheel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间轮服务类
 */
@Slf4j
@Service
public class TimingWheelService {

    private final TimingWheel timingWheel;
    private final Map<String, TimerTaskWrapper> activeTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskSequence = new AtomicLong(0);

    @Autowired
    public TimingWheelService(TimingWheel timingWheel) {
        this.timingWheel = timingWheel;
    }

    /**
     * 添加定时任务
     */
    public String scheduleTask(TimerTask task, long delayMs) {
        try {
            TimerTaskWrapper wrapper = timingWheel.schedule(task, delayMs);
            if (wrapper != null) {
                activeTasks.put(wrapper.getTaskId(), wrapper);
                log.info("Scheduled task: {}, delay: {}ms", wrapper.getTaskId(), delayMs);
                return wrapper.getTaskId();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to schedule task", e);
            throw new RuntimeException("Failed to schedule task", e);
        }
    }

    /**
     * 添加延迟任务（使用Lambda）
     */
    public String scheduleTask(Runnable task, long delayMs, String description) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
        return scheduleTask(timerTask, delayMs);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        boolean cancelled = timingWheel.cancelTask(taskId);
        if (cancelled) {
            activeTasks.remove(taskId);
            log.info("Cancelled task: {}", taskId);
        }
        return cancelled;
    }

    /**
     * 获取任务信息
     */
    public TimerTaskWrapper getTaskInfo(String taskId) {
        return activeTasks.get(taskId);
    }

    /**
     * 获取所有活跃任务
     */
    public List<TimerTaskWrapper> getActiveTasks() {
        return List.copyOf(activeTasks.values());
    }

    /**
     * 获取时间轮统计信息
     */
    public TimingWheel.TimingWheelStats getStats() {
        return timingWheel.getStats();
    }

    /**
     * 清理已完成的任务
     */
    public int cleanupCompletedTasks() {
        final AtomicInteger removedCount = new AtomicInteger(0);
        activeTasks.entrySet().removeIf(entry -> {
            TimerTaskWrapper wrapper = entry.getValue();
            boolean shouldRemove = wrapper.getStatus() == TimerTaskWrapper.TaskStatus.COMPLETED ||
                                 wrapper.getStatus() == TimerTaskWrapper.TaskStatus.FAILED ||
                                 wrapper.getStatus() == TimerTaskWrapper.TaskStatus.CANCELLED;
            if (shouldRemove) {
                removedCount.incrementAndGet();
            }
            return shouldRemove;
        });

        int count = removedCount.get();
        if (count > 0) {
            log.info("Cleaned up {} completed tasks", count);
        }

        return count;
    }

    /**
     * 获取任务执行统计
     */
    public TaskExecutionStats getExecutionStats() {
        Map<TimerTaskWrapper.TaskStatus, Long> statusCounts = activeTasks.values()
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(
                wrapper -> wrapper.getStatus(),
                java.util.stream.Collectors.counting()
            ));

        return new TaskExecutionStats(
            activeTasks.size(),
            statusCounts.getOrDefault(TimerTaskWrapper.TaskStatus.PENDING, 0L),
            statusCounts.getOrDefault(TimerTaskWrapper.TaskStatus.RUNNING, 0L),
            statusCounts.getOrDefault(TimerTaskWrapper.TaskStatus.COMPLETED, 0L),
            statusCounts.getOrDefault(TimerTaskWrapper.TaskStatus.FAILED, 0L),
            statusCounts.getOrDefault(TimerTaskWrapper.TaskStatus.CANCELLED, 0L),
            LocalDateTime.now()
        );
    }

    /**
     * 创建示例任务
     */
    public String createSampleTask(String type, long delayMs) {
        return scheduleTask(() -> {
            switch (type.toLowerCase()) {
                case "simple":
                    log.info("Simple task executed at {}", LocalDateTime.now());
                    break;
                case "calculation":
                    performCalculation();
                    break;
                case "io":
                    performIOOperation();
                    break;
                default:
                    log.info("Unknown task type: {} executed at {}", type, LocalDateTime.now());
            }
        }, delayMs, "Sample " + type + " task");
    }

    private void performCalculation() {
        // 模拟CPU密集型任务
        long result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += i;
        }
        log.info("Calculation task completed, result: {}", result);
    }

    private void performIOOperation() {
        // 模拟IO操作
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("IO operation task completed");
    }

    /**
     * 批量创建测试任务
     */
    public List<String> createBatchTasks(int count, long minDelay, long maxDelay) {
        List<String> taskIds = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            long delay = minDelay + (long) (Math.random() * (maxDelay - minDelay));
            String taskId = createSampleTask("simple", delay);
            if (taskId != null) {
                taskIds.add(taskId);
            }
        }
        log.info("Created {} batch tasks", taskIds.size());
        return taskIds;
    }

    /**
     * 任务执行统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TaskExecutionStats {
        private int totalTasks;
        private long pendingTasks;
        private long runningTasks;
        private long completedTasks;
        private long failedTasks;
        private long cancelledTasks;
        private LocalDateTime timestamp;
    }
}