package com.example.cli.service;

import com.example.cli.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 系统服务示例
 */
@Service("systemService")
public class SystemService implements CommandHandler {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Override
    public String handle(String[] args) {
        if (args.length == 0) {
            return getUsage();
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "status":
                return getSystemStatus();
            case "info":
                return getSystemInfo();
            case "time":
                return getCurrentTime(args.length > 1 && "utc".equalsIgnoreCase(args[1]));
            case "memory":
                return getMemoryInfo();
            case "version":
                return getVersion();
            default:
                return "未知命令: " + command + "\n" + getUsage();
        }
    }

    private String getSystemStatus() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        return String.format("""
                系统状态
                --------
                操作系统: %s %s
                可用处理器: %d
                系统负载: %.2f%%
                Java版本: %s
                JVM运行时间: %s
                """,
                osBean.getName(), osBean.getVersion(),
                runtime.availableProcessors(),
                osBean.getSystemLoadAverage() * 100,
                System.getProperty("java.version"),
                formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
    }

    private String getSystemInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        return String.format("""
                系统信息
                --------
                操作系统: %s
                系统版本: %s
                系统架构: %s
                可用处理器数: %d
                系统负载平均值: %.2f
                """,
                osBean.getName(),
                osBean.getVersion(),
                System.getProperty("os.arch"),
                osBean.getAvailableProcessors(),
                osBean.getSystemLoadAverage());
    }

    private String getCurrentTime(boolean utc) {
        LocalDateTime now = LocalDateTime.now();
        if (utc) {
            return "当前时间 (UTC): " + now.atZone(TimeZone.getTimeZone("UTC").toZoneId())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else {
            return "当前时间 (本地): " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    private String getMemoryInfo() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return String.format("""
                内存信息
                --------
                JVM最大内存: %s
                JVM总内存: %s
                已使用内存: %s
                空闲内存: %s
                内存使用率: %.1f%%
                堆内存使用: %s
                堆内存最大: %s
                """,
                formatBytes(maxMemory),
                formatBytes(totalMemory),
                formatBytes(usedMemory),
                formatBytes(freeMemory),
                (double) usedMemory / maxMemory * 100,
                formatBytes(memoryBean.getHeapMemoryUsage().getUsed()),
                formatBytes(memoryBean.getHeapMemoryUsage().getMax()));
    }

    private String getVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append("版本信息:\n");
        sb.append("-".repeat(30)).append("\n");

        if (buildProperties != null) {
            sb.append(String.format("应用版本: %s\n", buildProperties.getVersion()));
            sb.append(String.format("构建时间: %s\n", buildProperties.getTime()));
        } else {
            sb.append("应用版本: 未知\n");
        }

        sb.append(String.format("Spring Boot版本: %s\n",
            org.springframework.boot.SpringBootVersion.getVersion()));
        sb.append(String.format("Java版本: %s\n", System.getProperty("java.version")));

        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatUptime(long uptimeMs) {
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (uptimeMs % (1000 * 60)) / 1000;

        return String.format("%d小时%d分钟%d秒", hours, minutes, seconds);
    }

    @Override
    public String getDescription() {
        return "系统信息和服务监控";
    }

    @Override
    public String getUsage() {
        return """
                系统服务使用说明:

                命令格式: systemService <command> [args]

                可用命令:
                  status        - 获取系统状态
                  info          - 获取系统信息
                  time [utc]    - 获取当前时间（加utc参数显示UTC时间）
                  memory        - 获取内存使用情况
                  version       - 获取版本信息

                示例:
                  systemService status     - 获取系统状态
                  systemService time       - 获取本地时间
                  systemService time utc   - 获取UTC时间
                  systemService memory     - 获取内存信息
                """;
    }
}