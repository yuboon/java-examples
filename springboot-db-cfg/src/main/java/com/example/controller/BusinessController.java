package com.example.controller;

import com.example.service.BusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务控制器示例 - 展示如何使用@Autowired直接注入默认数据源
 */
@RestController
@RequestMapping("/api/business")
@Slf4j
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    // 直接使用@Autowired注入默认数据源（businessDataSource）
    @Autowired
    private DataSource dataSource;

    // 直接使用@Autowired注入默认JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 健康检查 - 测试默认数据源连接
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(businessService.testConnection());
    }

    /**
     * 获取数据源信息
     */
    @GetMapping("/datasource-info")
    public ResponseEntity<Map<String, Object>> getDataSourceInfo() {
        Map<String, Object> info = businessService.getDataSourceInfo();
        info.put("message", "这是通过@Autowired直接注入的默认数据源信息");
        return ResponseEntity.ok(info);
    }

    /**
     * 获取业务数据
     */
    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> getBusinessData() {
        List<Map<String, Object>> data = businessService.getBusinessData();
        return ResponseEntity.ok(data);
    }

    /**
     * 插入业务数据
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> insertBusinessData(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        
        boolean success = businessService.insertBusinessData(name, description != null ? description : "");
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "数据插入成功" : "数据插入失败（可能表不存在）",
            "name", name,
            "description", description != null ? description : ""
        ));
    }

    /**
     * 直接执行SQL测试
     */
    @GetMapping("/sql-test")
    public ResponseEntity<Map<String, Object>> sqlTest() {
        try {
            // 直接使用注入的JdbcTemplate执行查询
            String currentTime = jdbcTemplate.queryForObject("SELECT NOW() as current_time", String.class);
            String version = jdbcTemplate.queryForObject("SELECT VERSION() as version", String.class);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "currentTime", currentTime,
                "databaseVersion", version,
                "message", "使用@Autowired注入的默认JdbcTemplate执行成功"
            ));
        } catch (Exception e) {
            log.error("SQL测试执行失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取早期配置加载情况
     */
    @GetMapping("/early-config-status")
    public ResponseEntity<Map<String, Object>> getEarlyConfigStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 检查关键的早期配置是否已加载
            String serverPort = System.getProperty("server.port");
            String envServerPort = System.getenv("SERVER_PORT");
            String actualServerPort = serverPort != null ? serverPort : envServerPort;
            
            status.put("earlyConfigLoaded", true);
            status.put("serverPort", actualServerPort != null ? actualServerPort : "8080 (default)");
            status.put("datasourceUrl", businessService.getDataSourceInfo().get("jdbcUrl"));
            status.put("configLoadTime", "Early initialization phase (ApplicationContextInitializer)");
            
            // 添加数据源信息
            Map<String, Object> dsInfo = businessService.getDataSourceInfo();
            status.put("businessDataSourceClass", dsInfo.get("dataSourceClass"));
            status.put("isDatabaseConfigured", dsInfo.get("jdbcUrl") != null && !dsInfo.get("jdbcUrl").toString().contains("business_db"));
            
            status.put("message", "早期配置加载状态检查");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取早期配置状态失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}