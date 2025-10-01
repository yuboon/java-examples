package com.license.controller;

import com.license.annotation.RequireFeature;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 报表控制器
 * 演示功能权限控制的使用
 */
@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class ReportController {

    /**
     * 导出报表功能 - 需要REPORT_EXPORT权限
     */
    @GetMapping("/export")
    @RequireFeature(value = "REPORT_EXPORT", message = "报表导出功能未授权")
    public ResponseEntity<Map<String, Object>> exportReport(@RequestParam(defaultValue = "pdf") String format) {
        // 模拟报表导出功能
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "报表导出成功");
        response.put("format", format);
        response.put("fileName", "report_" + System.currentTimeMillis() + "." + format);
        response.put("size", "2.5 MB");

        return ResponseEntity.ok(response);
    }

    /**
     * 定时报表功能 - 需要REPORT_SCHEDULE权限
     */
    @PostMapping("/schedule")
    @RequireFeature(value = "REPORT_SCHEDULE", message = "定时报表功能未授权")
    public ResponseEntity<Map<String, Object>> scheduleReport(@RequestBody Map<String, Object> request) {
        // 模拟定时报表创建
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "定时报表创建成功");
        response.put("scheduleId", "SCH-" + System.currentTimeMillis());
        response.put("cron", request.getOrDefault("cron", "0 0 9 * * ?"));

        return ResponseEntity.ok(response);
    }

    /**
     * 数据分析功能 - 需要DATA_ANALYSIS权限
     */
    @GetMapping("/analysis")
    @RequireFeature(value = "DATA_ANALYSIS", message = "数据分析功能未授权")
    public ResponseEntity<Map<String, Object>> dataAnalysis(@RequestParam(required = false) String dimension) {
        // 模拟数据分析
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数据分析完成");
        response.put("dimension", dimension != null ? dimension : "default");

        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("totalRecords", 15234);
        analysisResult.put("avgValue", 456.78);
        analysisResult.put("maxValue", 1234.56);
        analysisResult.put("minValue", 12.34);

        response.put("result", analysisResult);

        return ResponseEntity.ok(response);
    }

    /**
     * 用户管理功能 - 需要USER_MANAGEMENT权限
     */
    @GetMapping("/users")
    @RequireFeature(value = "USER_MANAGEMENT", message = "用户管理功能未授权")
    public ResponseEntity<Map<String, Object>> getUserList() {
        // 模拟用户列表查询
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "用户列表查询成功");
        response.put("totalCount", 245);
        response.put("currentPage", 1);
        response.put("pageSize", 20);

        return ResponseEntity.ok(response);
    }

    /**
     * 查看报表 - 无需特定权限
     */
    @GetMapping("/view")
    public ResponseEntity<Map<String, Object>> viewReport(@RequestParam(defaultValue = "1") String reportId) {
        // 基础查看功能，不需要额外权限
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "报表查看成功");
        response.put("reportId", reportId);
        response.put("reportName", "月度销售报表");
        response.put("createDate", "2025-10-01");

        return ResponseEntity.ok(response);
    }
}
