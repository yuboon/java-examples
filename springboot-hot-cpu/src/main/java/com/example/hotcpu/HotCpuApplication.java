package com.example.hotcpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
@EnableScheduling
public class HotCpuApplication {

    private static final Logger logger = LoggerFactory.getLogger(HotCpuApplication.class);

    public static void main(String[] args) {
        System.setProperty("spring.output.ansi.enabled", "always");
        
        ConfigurableApplicationContext context = SpringApplication.run(HotCpuApplication.class, args);
        
        String port = context.getEnvironment().getProperty("server.port", "8080");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "") + "index.html";
        
        logger.info("🚀 Spring Boot Hot CPU Analyzer started successfully!");
        logger.info("🔥 Flame Graph UI: http://localhost:{}{}", port, contextPath);
        logger.info("📊 API Endpoints:");
        logger.info("   - GET  /api/flamegraph        - 获取火焰图数据");
        logger.info("   - POST /api/sampling/enable   - 启用CPU采样");
        logger.info("   - POST /api/sampling/disable  - 禁用CPU采样");
        logger.info("   - GET  /api/sampling/status   - 查看采样状态");
        logger.info("   - POST /api/sampling/clear    - 清空采样数据");
        logger.info("🧪 Test Endpoints:");
        logger.info("   - GET  /test/cpu-intensive    - CPU密集型任务测试");
        logger.info("   - GET  /test/nested-calls     - 嵌套调用测试");
        logger.info("   - GET  /test/mixed-workload   - 混合工作负载测试");
        
        // 尝试自动打开浏览器
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port + contextPath + "/"));
                logger.info("🖥️  Browser opened automatically");
            }
        } catch (Exception e) {
            logger.debug("Could not open browser automatically: {}", e.getMessage());
        }
    }
}