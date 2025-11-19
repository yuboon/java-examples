package com.example.dfa.controller;

import com.example.dfa.entity.SensitiveWordResult;
import com.example.dfa.service.SensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词过滤器 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/sensitive-word")
@CrossOrigin(origins = "*")
public class SensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 检查文本是否包含敏感词
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkSensitiveWord(@RequestParam String text) {
        try {
            boolean hasSensitive = sensitiveWordService.containsSensitiveWord(text);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasSensitive", hasSensitive);
            response.put("text", text);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查敏感词失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "检查失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 过滤文本中的敏感词
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterText(
            @RequestParam String text,
            @RequestParam(defaultValue = "*") String replacement) {
        try {
            SensitiveWordService.FilterResult result =
                sensitiveWordService.getFilterResult(text, replacement);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalText", result.getOriginalText());
            response.put("filteredText", result.getFilteredText());
            response.put("hasSensitive", result.isHasSensitive());
            response.put("sensitiveWordCount", result.getSensitiveWordCount());
            response.put("sensitiveWords", result.getSensitiveWords());
            response.put("replacement", replacement);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("过滤敏感词失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "过滤失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查找文本中的所有敏感词
     */
    @GetMapping("/find-all")
    public ResponseEntity<Map<String, Object>> findAllSensitiveWords(@RequestParam String text) {
        try {
            List<SensitiveWordResult> sensitiveWords =
                sensitiveWordService.findAllSensitiveWords(text);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", text);
            response.put("sensitiveWords", sensitiveWords);
            response.put("count", sensitiveWords.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查找敏感词失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "查找失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 添加敏感词到词库
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addSensitiveWord(@RequestBody Map<String, String> request) {
        try {
            String word = request.get("word");
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "敏感词不能为空"
                ));
            }

            sensitiveWordService.addSensitiveWord(word);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "敏感词添加成功");
            response.put("word", word);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("添加敏感词失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "添加失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量添加敏感词
     */
    @PostMapping("/add-batch")
    public ResponseEntity<Map<String, Object>> addSensitiveWords(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) request.get("words");

            if (words == null || words.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "敏感词列表不能为空"
                ));
            }

            sensitiveWordService.addSensitiveWords(words);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量添加成功");
            response.put("count", words.size());
            response.put("words", words);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量添加敏感词失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "批量添加失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 重新加载敏感词库
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadSensitiveWords(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) request.get("words");

            if (words == null || words.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "敏感词列表不能为空"
                ));
            }

            sensitiveWordService.reloadSensitiveWords(words);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "词库重新加载成功");
            response.put("count", words.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("重新加载敏感词库失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "重新加载失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", "running");
            response.put("algorithm", "DFA (Deterministic Finite Automaton)");
            response.put("dataStructure", "Trie Tree");
            response.put("features", List.of(
                "高效敏感词检测",
                "实时文本过滤",
                "批量敏感词管理",
                "前缀共享优化",
                "线性时间复杂度"
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取状态失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "DFA Sensitive Word Filter");

        return ResponseEntity.ok(response);
    }
}