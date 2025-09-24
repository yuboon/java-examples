package com.example.dynamicrule.service;

import com.example.dynamicrule.entity.RuleExecuteResponse;
import com.example.dynamicrule.entity.RuleScript;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DynamicRuleEngine {

    // TODO 为方便demo演示，规则存于map, 实际项目使用需要存储在数据库或者其他数据存储组件中
    private final Map<String, RuleScript> ruleCache = new ConcurrentHashMap<>();
    private final ExpressRunner expressRunner = new ExpressRunner();

    @PostConstruct
    public void init() throws Exception {
        initDefaultRules();
        ExpressRunner runner = new ExpressRunner();

        /* 1. 预编译（可放在系统启动阶段） */
        //String expr = "price * (1 - discount)";
        //InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache(expr);
    }

    private void initDefaultRules() {
        addRule(new RuleScript("vip_discount",
                "if (userLevel == \"GOLD\") { return price * 0.8; } else if (userLevel == \"SILVER\") { return price * 0.9; } else { return price; }",
                "VIP用户折扣规则"));

        addRule(new RuleScript("full_reduction",
                "if (totalAmount >= 200) { return totalAmount - 50; } else if (totalAmount >= 100) { return totalAmount - 20; } else { return totalAmount; }",
                "满减活动规则"));

        addRule(new RuleScript("points_reward",
                "return Math.round(totalAmount * 0.1);",
                "积分奖励规则"));
    }

    public void addRule(RuleScript ruleScript) {
        if (ruleScript.getCreateTime() == null) {
            ruleScript.setCreateTime(LocalDateTime.now());
        }
        ruleScript.setUpdateTime(LocalDateTime.now());
        ruleCache.put(ruleScript.getRuleName(), ruleScript);
        log.info("添加规则: {}", ruleScript.getRuleName());
    }

    public void updateRule(String ruleName, RuleScript updatedRule) {
        if (!ruleCache.containsKey(ruleName)) {
            throw new RuntimeException("规则不存在: " + ruleName);
        }

        RuleScript existingRule = ruleCache.get(ruleName);
        existingRule.setScript(updatedRule.getScript());
        existingRule.setDescription(updatedRule.getDescription());
        existingRule.setEnabled(updatedRule.isEnabled());
        existingRule.setUpdateTime(LocalDateTime.now());

        log.info("更新规则: {}", ruleName);
    }

    public void deleteRule(String ruleName) {
        if (ruleCache.remove(ruleName) != null) {
            log.info("删除规则: {}", ruleName);
        } else {
            throw new RuntimeException("规则不存在: " + ruleName);
        }
    }

    public RuleScript getRule(String ruleName) {
        return ruleCache.get(ruleName);
    }

    public List<RuleScript> getAllRules() {
        return new ArrayList<>(ruleCache.values());
    }

    public RuleExecuteResponse executeRule(String ruleName, Map<String, Object> params) {
        try {
            RuleScript rule = ruleCache.get(ruleName);
            if (rule == null) {
                return RuleExecuteResponse.error("规则不存在: " + ruleName);
            }

            if (!rule.isEnabled()) {
                return RuleExecuteResponse.error("规则已禁用: " + ruleName);
            }

            DefaultContext<String, Object> context = new DefaultContext<>();
            if (params != null) {
                params.forEach(context::put);
            }

            Object result = expressRunner.execute(rule.getScript(), context, null, true, false);
            log.info("执行规则: {}, 结果: {}", ruleName, result);

            return RuleExecuteResponse.success(result);
        } catch (Exception e) {
            log.error("执行规则失败: {}, 错误: {}", ruleName, e.getMessage(), e);
            return RuleExecuteResponse.error("执行失败: " + e.getMessage());
        }
    }
}