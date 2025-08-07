package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 业务服务示例 - 展示如何使用@Autowired直接注入默认数据源和JdbcTemplate
 */
@Service
@Slf4j
public class BusinessService {

    // 直接使用@Autowired注入默认数据源（businessDataSource）
    @Autowired
    private DataSource dataSource;

    // 直接使用@Autowired注入默认JdbcTemplate（使用businessDataSource）
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试数据源连接
     */
    public Map<String, Object> testConnection() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 'connection-ok' as result", String.class);
            return Map.of(
                "status", "success",
                "result", result,
                "dataSourceClass", dataSource.getClass().getSimpleName(),
                "message", "默认数据源连接正常"
            );
        } catch (Exception e) {
            log.error("数据源连接测试失败", e);
            return Map.of(
                "status", "error",
                "message", e.getMessage()
            );
        }
    }

    /**
     * 获取业务数据示例
     */
    public List<Map<String, Object>> getBusinessData() {
        try {
            // 这里可以查询实际的业务表，如果不存在则返回示例数据
            String sql = "SELECT 'sample-id' as id, 'sample-name' as name, NOW() as created_at";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("获取业务数据失败", e);
            // 返回示例数据
            return List.of(Map.of(
                "id", "sample-id",
                "name", "sample-name",
                "created_at", System.currentTimeMillis(),
                "note", "示例数据，因为业务表可能不存在"
            ));
        }
    }

    /**
     * 插入业务数据示例
     */
    public boolean insertBusinessData(String name, String description) {
        try {
            // 尝试插入到业务表，如果表不存在会报错
            String sql = "INSERT INTO business_table (name, description, created_at) VALUES (?, ?, NOW())";
            int rows = jdbcTemplate.update(sql, name, description);
            return rows > 0;
        } catch (Exception e) {
            log.warn("插入业务数据失败，可能是因为表不存在: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据源信息
     */
    public Map<String, Object> getDataSourceInfo() {
        try (var connection = dataSource.getConnection()) {
            return Map.of(
                "dataSourceClass", dataSource.getClass().getSimpleName(),
                "jdbcUrl", connection.getMetaData().getURL(),
                "username", connection.getMetaData().getUserName(),
                "databaseProductName", connection.getMetaData().getDatabaseProductName(),
                "driverVersion", connection.getMetaData().getDriverVersion(),
                "isValid", connection.isValid(5)
            );
        } catch (Exception e) {
            log.error("获取数据源信息失败", e);
            return Map.of(
                "error", e.getMessage(),
                "dataSourceClass", dataSource.getClass().getSimpleName()
            );
        }
    }
}