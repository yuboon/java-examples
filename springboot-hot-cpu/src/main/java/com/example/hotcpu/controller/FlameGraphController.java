package com.example.hotcpu.controller;

import com.example.hotcpu.service.CpuSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FlameGraphController {

    private static final Logger logger = LoggerFactory.getLogger(FlameGraphController.class);

    @Autowired
    private CpuSampler sampler;

    @GetMapping(value = "/flamegraph", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getFlameGraphData() {
        try {
            StringBuilder sb = new StringBuilder();
            Map<String, AtomicInteger> stackCount = sampler.getStackCount();
            
            if (stackCount.isEmpty()) {
                return ResponseEntity.ok("# No sampling data available. Make sure sampling is enabled.\n");
            }
            
            stackCount.forEach((stack, count) -> {
                if (stack != null && !stack.trim().isEmpty() && count.get() > 0) {
                    // 移除末尾的分号
                    String cleanStack = stack.endsWith(";") ? stack.substring(0, stack.length() - 1) : stack;
                    sb.append(cleanStack).append(" ").append(count.get()).append("\n");
                }
            });
            
            logger.debug("Generated flame graph data with {} entries", stackCount.size());
            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            logger.error("Error generating flame graph data", e);
            return ResponseEntity.internalServerError().body("Error generating flame graph data: " + e.getMessage());
        }
    }

    @PostMapping("/sampling/enable")
    public ResponseEntity<Map<String, Object>> enableSampling() {
        sampler.enableSampling();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "enabled");
        response.put("message", "CPU sampling has been enabled");
        logger.info("CPU sampling enabled via API");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sampling/disable")
    public ResponseEntity<Map<String, Object>> disableSampling() {
        sampler.disableSampling();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "disabled");
        response.put("message", "CPU sampling has been disabled");
        logger.info("CPU sampling disabled via API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sampling/status")
    public ResponseEntity<Map<String, Object>> getSamplingStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", sampler.isEnabled());
        response.put("stackCountSize", sampler.getStackCountSize());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sampling/clear")
    public ResponseEntity<Map<String, Object>> clearSamplingData() {
        sampler.clearData();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sampling data cleared successfully");
        logger.info("Sampling data cleared via API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sampling/debug")
    public ResponseEntity<Map<String, Object>> debugSampling() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", sampler.isEnabled());
        response.put("stackCountSize", sampler.getStackCountSize());
        
        // 获取前10条数据作为调试信息
        Map<String, AtomicInteger> stackCount = sampler.getStackCount();
        Map<String, Integer> sampleData = new HashMap<>();
        int count = 0;
        for (Map.Entry<String, AtomicInteger> entry : stackCount.entrySet()) {
            if (count >= 10) break;
            sampleData.put(entry.getKey(), entry.getValue().get());
            count++;
        }
        response.put("sampleData", sampleData);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "springboot-hot-cpu");
        response.put("sampling", sampler.isEnabled() ? "enabled" : "disabled");
        return ResponseEntity.ok(response);
    }
}