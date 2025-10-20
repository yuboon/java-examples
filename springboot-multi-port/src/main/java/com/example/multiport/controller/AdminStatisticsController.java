package com.example.multiport.controller;

import com.example.multiport.annotation.AdminApi;
import com.example.multiport.model.ApiResponse;
import com.example.multiport.service.ProductService;
import com.example.multiport.service.PortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端统计控制器
 * 提供数据统计功能
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@AdminApi
public class AdminStatisticsController {

    private final ProductService productService;
    private final PortService portService;

    /**
     * 获取商品统计信息
     */
    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> getProductStatistics() {
        log.info("管理端获取商品统计信息");
        Map<String, Object> statistics = productService.getStatistics();
        return ApiResponse.success("获取商品统计成功", statistics);
    }

    /**
     * 获取系统概览信息
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getSystemOverview() {
        log.info("管理端获取系统概览信息");

        Map<String, Object> productStats = productService.getStatistics();

        Map<String, Object> overview = new HashMap<>();
        overview.put("products", productStats);
        overview.put("system", getSystemInfo());
        overview.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success("获取系统概览成功", overview);
    }

    /**
     * 获取系统信息
     */
    private Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", runtime.availableProcessors());
        systemInfo.put("totalMemory", runtime.totalMemory());
        systemInfo.put("freeMemory", runtime.freeMemory());
        systemInfo.put("maxMemory", runtime.maxMemory());

        return systemInfo;
    }

    /**
     * 获取端口状态信息
     */
    @GetMapping("/ports")
    public ApiResponse<Map<String, Object>> getPortStatus() {
        log.info("管理端获取端口状态信息");

        Map<String, Object> portStatus = new HashMap<>();
        portStatus.put("userPort", portService.getUserPort());
        portStatus.put("adminPort", portService.getAdminPort());
        portStatus.put("userPortStatus", "ACTIVE");
        portStatus.put("adminPortStatus", "ACTIVE");

        return ApiResponse.success("获取端口状态成功", portStatus);
    }
}