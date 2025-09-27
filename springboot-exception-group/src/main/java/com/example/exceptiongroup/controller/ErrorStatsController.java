package com.example.exceptiongroup.controller;

import com.example.exceptiongroup.cache.ErrorFingerprintCache;
import com.example.exceptiongroup.fingerprint.ErrorFingerprintGenerator;
import com.example.exceptiongroup.util.TraceIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 错误统计REST API控制器
 */
@RestController
@RequestMapping("/api/error-stats")
@CrossOrigin(origins = "*")
public class ErrorStatsController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorStatsController.class);

    @Autowired
    private ErrorFingerprintCache fingerprintCache;

    @Autowired
    private TraceIdGenerator traceIdGenerator;

    /**
     * 获取所有错误指纹统计
     */
    @GetMapping("/fingerprints")
    public ResponseEntity<List<ErrorFingerprintCache.ErrorFingerprint>> getAllFingerprints() {
        logger.info("Getting all fingerprints, cache size: {}", fingerprintCache.size());
        List<ErrorFingerprintCache.ErrorFingerprint> fingerprints = fingerprintCache.getAllFingerprints();
        logger.info("Returning {} fingerprints", fingerprints.size());
        return ResponseEntity.ok(fingerprints);
    }

    /**
     * 根据指纹ID获取详细信息
     */
    @GetMapping("/fingerprints/{fingerprint}")
    public ResponseEntity<ErrorFingerprintCache.ErrorFingerprint> getFingerprint(@PathVariable("fingerprint") String fingerprint) {
        ErrorFingerprintCache.ErrorFingerprint fp = fingerprintCache.getFingerprint(fingerprint);
        if (fp != null) {
            return ResponseEntity.ok(fp);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取错误统计概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        List<ErrorFingerprintCache.ErrorFingerprint> fingerprints = fingerprintCache.getAllFingerprints();

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalFingerprints", fingerprints.size());
        overview.put("totalErrors", fingerprints.stream().mapToLong(fp -> fp.getCount()).sum());
        overview.put("cacheCapacity", fingerprintCache.size());
        overview.put("timestamp", LocalDateTime.now());

        logger.info("Overview: {} fingerprints, {} total errors", fingerprints.size(),
                    fingerprints.stream().mapToLong(fp -> fp.getCount()).sum());

        // 统计最频繁的错误
        ErrorFingerprintCache.ErrorFingerprint mostFrequent = fingerprints.stream()
            .max((a, b) -> Long.compare(a.getCount(), b.getCount()))
            .orElse(null);

        if (mostFrequent != null) {
            Map<String, Object> mostFrequentInfo = new HashMap<>();
            mostFrequentInfo.put("fingerprint", mostFrequent.getFingerprint());
            mostFrequentInfo.put("count", mostFrequent.getCount());
            overview.put("mostFrequentError", mostFrequentInfo);
        }

        return ResponseEntity.ok(overview);
    }

    /**
     * 清空错误统计缓存
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        fingerprintCache.clear();
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Error fingerprint cache cleared");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/simulate/{errorType}")
    public ResponseEntity<Map<String, String>> simulateError(@PathVariable("errorType") String errorType) {
        String traceId = traceIdGenerator.generateTraceId();
        Map<String, String> result = new HashMap<>();

        logger.info("Simulating error type: {} with traceId: {}", errorType, traceId);

        try {
            switch (errorType.toLowerCase()) {
                case "npe":
                    simulateNullPointerException();
                    break;
                case "iae":
                    simulateIllegalArgumentException();
                    break;
                case "ioexception":
                    simulateIOException();
                    break;
                default:
                    simulateGenericException();
            }
        } catch (Exception e) {
            // 既记录日志又直接处理，确保数据被添加
            logger.error("Simulated error occurred for type: " + errorType, e);

            // 直接调用指纹处理，绕过Logback问题
            try {
                ErrorFingerprintGenerator generator = new ErrorFingerprintGenerator();
                String fingerprintStr = generator.generateFingerprint(e);

                // 获取堆栈跟踪信息
                String stackTrace = getStackTraceString(e);

                ErrorFingerprintCache.getInstance().shouldLog(
                    fingerprintStr, traceId, e.getClass().getSimpleName(), stackTrace);
            } catch (Exception ex) {
                logger.warn("Failed to process fingerprint directly: " + ex.getMessage());
            }

            result.put("status", "error_simulated");
            result.put("errorType", e.getClass().getSimpleName());
            result.put("traceId", traceId);
            result.put("message", e.getMessage());
            result.put("cacheSize", String.valueOf(fingerprintCache.size()));
            return ResponseEntity.ok(result);
        }

        result.put("status", "no_error_occurred");
        result.put("traceId", traceId);
        return ResponseEntity.ok(result);
    }

    private void simulateNullPointerException() {
        String nullString = null;
        nullString.length(); // 故意触发NPE
    }

    private void simulateIllegalArgumentException() {
        throw new IllegalArgumentException("Invalid argument provided for simulation");
    }

    private void simulateIOException() throws java.io.IOException {
        throw new java.io.IOException("Simulated IO operation failed");
    }

    private void simulateGenericException() {
        throw new RuntimeException("Generic runtime exception for testing");
    }

    /**
     * 获取异常的堆栈跟踪字符串
     */
    private String getStackTraceString(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}