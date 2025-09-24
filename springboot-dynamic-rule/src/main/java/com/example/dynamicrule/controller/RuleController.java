package com.example.dynamicrule.controller;

import com.example.dynamicrule.entity.RuleExecuteRequest;
import com.example.dynamicrule.entity.RuleExecuteResponse;
import com.example.dynamicrule.entity.RuleScript;
import com.example.dynamicrule.service.DynamicRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final DynamicRuleEngine dynamicRuleEngine;

    @PostMapping
    public ResponseEntity<String> addRule(@RequestBody RuleScript ruleScript) {
        try {
            dynamicRuleEngine.addRule(ruleScript);
            return ResponseEntity.ok("规则添加成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加失败: " + e.getMessage());
        }
    }

    @PutMapping("/{ruleName}")
    public ResponseEntity<String> updateRule(@PathVariable String ruleName, @RequestBody RuleScript ruleScript) {
        try {
            dynamicRuleEngine.updateRule(ruleName, ruleScript);
            return ResponseEntity.ok("规则更新成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{ruleName}")
    public ResponseEntity<String> deleteRule(@PathVariable String ruleName) {
        try {
            dynamicRuleEngine.deleteRule(ruleName);
            return ResponseEntity.ok("规则删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/{ruleName}")
    public ResponseEntity<RuleScript> getRule(@PathVariable String ruleName) {
        RuleScript rule = dynamicRuleEngine.getRule(ruleName);
        if (rule != null) {
            return ResponseEntity.ok(rule);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<RuleScript>> getAllRules() {
        return ResponseEntity.ok(dynamicRuleEngine.getAllRules());
    }

    @PostMapping("/execute")
    public ResponseEntity<RuleExecuteResponse> executeRule(@RequestBody RuleExecuteRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getParams();
        RuleExecuteResponse response = dynamicRuleEngine.executeRule(request.getRuleName(), params);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute/{ruleName}")
    public ResponseEntity<RuleExecuteResponse> executeRuleByName(
            @PathVariable String ruleName,
            @RequestBody Map<String, Object> params) {
        RuleExecuteResponse response = dynamicRuleEngine.executeRule(ruleName, params);
        return ResponseEntity.ok(response);
    }
}