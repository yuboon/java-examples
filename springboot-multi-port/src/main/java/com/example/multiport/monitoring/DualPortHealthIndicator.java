package com.example.multiport.monitoring;

import com.example.multiport.service.PortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 双端口健康检查指示器
 * 检查两个端口的服务状态
 */
@Component
public class DualPortHealthIndicator implements HealthIndicator {

    @Autowired
    private PortService portService;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // 构建动态URL
        String userPortUrl = portService.getUserUrlPrefix() + "/api/user/products";
        String adminPortUrl = portService.getAdminUrlPrefix() + "/api/admin/products";

        // 检查用户端端口
        boolean userPortHealthy = checkPortHealth(userPortUrl, "用户端");
        if (!userPortHealthy) {
            builder = Health.down();
        }

        // 检查管理端端口
        boolean adminPortHealthy = checkPortHealth(adminPortUrl, "管理端");
        if (!adminPortHealthy) {
            builder = Health.down();
        }

        return builder
                .withDetail("user-port", userPortHealthy ? "UP" : "DOWN")
                .withDetail("admin-port", adminPortHealthy ? "UP" : "DOWN")
                .withDetail("user-port-url", userPortUrl)
                .withDetail("admin-port-url", adminPortUrl)
                .withDetail("status", userPortHealthy && adminPortHealthy ?
                        "Both ports are active" : "One or more ports are down")
                .build();
    }

    /**
     * 检查指定端口URL的健康状态
     */
    private boolean checkPortHealth(String url, String portName) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            boolean isHealthy = responseCode >= 200 && responseCode < 300;

            if (!isHealthy) {
                System.err.printf("健康检查失败 - %s: HTTP %d%n", portName, responseCode);
            }

            return isHealthy;

        } catch (ConnectException e) {
            System.err.printf("健康检查失败 - %s: 连接被拒绝%n", portName);
            return false;
        } catch (Exception e) {
            System.err.printf("健康检查失败 - %s: %s%n", portName, e.getMessage());
            return false;
        }
    }
}