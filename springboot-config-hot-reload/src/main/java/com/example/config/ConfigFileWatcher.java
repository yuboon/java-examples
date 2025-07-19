package com.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ConfigFileWatcher {
    // 监听的配置文件路径（默认监听classpath下的application.yaml）
    private final String configPath = "classpath:application.yaml";
    private WatchService watchService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConfigRefreshHandler refreshHandler;
    private long lastProcessTime;
    private final long EVENT_DEBOUNCE_TIME = 500; // 500毫秒防抖时间

    // 注入配置刷新处理器（后面实现）
    public ConfigFileWatcher(ConfigRefreshHandler refreshHandler) {
        this.refreshHandler = refreshHandler;
    }

    @PostConstruct
    public void init() throws IOException {
        // 获取配置文件的实际路径
        Resource resource = new FileSystemResource(ResourceUtils.getFile(configPath));
        Path configDir = resource.getFile().toPath().getParent(); // 监听配置文件所在目录
        String fileName = resource.getFilename(); // 配置文件名（如application.yaml）

        watchService = FileSystems.getDefault().newWatchService();
        // 注册文件修改事件（ENTRY_MODIFY）
        configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        // 启动线程监听文件变化
        executor.submit(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take(); // 阻塞等待事件
                    // 防抖检查：忽略短时间内重复事件
                    if (System.currentTimeMillis() - lastProcessTime < EVENT_DEBOUNCE_TIME) {
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue; // 事件溢出，忽略
                        }

                        // 检查是否是目标配置文件被修改
                        Path changedFile = (Path) event.context();
                        if (changedFile.getFileName().toString().equals(fileName)) {
                            log.info("检测到配置文件修改：{}", fileName);
                            refreshHandler.refresh(); // 触发配置刷新
                        }
                    }
                    boolean valid = key.reset(); // 重置监听器
                    if (!valid) break; // 监听器失效，退出循环
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        log.info("配置文件监听器启动成功，监听路径：{}", configDir);
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
        try {
            watchService.close();
        } catch (IOException e) {
            log.error("关闭WatchService失败", e);
        }
    }
}