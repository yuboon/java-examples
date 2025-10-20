package com.example.multiport.controller;

import com.example.multiport.logging.PortAwareLogger;
import com.example.multiport.model.ApiResponse;
import com.example.multiport.service.ProductService;
import com.example.multiport.service.PortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 为不同端口提供独立的健康检查端点
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final ProductService productService;
    private final PortAwareLogger portAwareLogger;
    private final PortService portService;

    /**
     * 用户端健康检查
     */
    @GetMapping("/user")
    public ApiResponse<Map<String, Object>> userHealth(HttpServletRequest request) {
        int port = request.getLocalPort();
        portAwareLogger.logBusinessOperation(port, "HEALTH_CHECK", "用户端健康检查");

        Map<String, Object> health = new HashMap<>();
        health.put("port", port);
        health.put("status", "UP");
        health.put("service", "user-api");
        health.put("timestamp", System.currentTimeMillis());
        health.put("productCount", productService.getAllProductsForUser().size());

        return ApiResponse.success("用户端服务健康", health);
    }

    /**
     * 管理端健康检查
     */
    @GetMapping("/admin")
    public ApiResponse<Map<String, Object>> adminHealth(HttpServletRequest request) {
        int port = request.getLocalPort();
        portAwareLogger.logBusinessOperation(port, "HEALTH_CHECK", "管理端健康检查");

        Map<String, Object> health = new HashMap<>();
        health.put("port", port);
        health.put("status", "UP");
        health.put("service", "admin-api");
        health.put("timestamp", System.currentTimeMillis());
        health.put("productCount", productService.getAllProductsForAdmin().size());

        Map<String, Object> statistics = productService.getStatistics();
        health.put("statistics", statistics);

        return ApiResponse.success("管理端服务健康", health);
    }

    /**
     * 通用健康检查（兼容性端点）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> generalHealth(HttpServletRequest request) {
        int port = request.getLocalPort();
        String serviceType = portService.getServiceTypePrefix(port);

        Map<String, Object> health = new HashMap<>();
        health.put("port", port);
        health.put("status", "UP");
        health.put("service", serviceType + "-api");
        health.put("timestamp", System.currentTimeMillis());

        if (portService.isUserPort(port)) {
            health.put("endpoint", "用户端健康检查端点");
        } else {
            health.put("endpoint", "管理端健康检查端点");
        }

        return ApiResponse.success("服务健康", health);
    }
}