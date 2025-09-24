package com.example.dynamicrule.service;

import com.example.dynamicrule.entity.Order;
import com.example.dynamicrule.entity.OrderProcessResult;
import com.example.dynamicrule.entity.RuleExecuteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DynamicRuleEngine ruleEngine;

    public OrderProcessResult processOrder(Order order) {
        log.info("开始处理订单: {}", order.getOrderId());

        List<OrderProcessResult.ProcessStep> steps = new ArrayList<>();
        BigDecimal currentAmount = order.getOriginalAmount();

        // 1. 应用VIP折扣规则
        BigDecimal discountedAmount = applyVipDiscount(order, currentAmount, steps);

        // 2. 应用满减规则
        BigDecimal finalAmount = applyFullReduction(order, discountedAmount, steps);

        // 3. 计算积分奖励
        Integer points = calculatePoints(finalAmount, order);

        // 4. 创建处理结果
        OrderProcessResult result = new OrderProcessResult();
        result.setOrderId(order.getOrderId());
        result.setUserId(order.getUserId());
        result.setUserLevel(order.getUserLevel());
        result.setOriginalAmount(order.getOriginalAmount());
        result.setFinalAmount(finalAmount);
        result.setPointsEarned(points);
        result.setCreateTime(order.getCreateTime());
        result.setStatus("PROCESSED");
        result.setProcessSteps(steps);

        log.info("订单处理完成: {} -> 原价: {}, 最终价格: {}, 获得积分: {}",
                order.getOrderId(), order.getOriginalAmount(), finalAmount, points);

        return result;
    }

    private BigDecimal applyVipDiscount(Order order, BigDecimal currentAmount, List<OrderProcessResult.ProcessStep> steps) {
        Map<String, Object> params = new HashMap<>();
        params.put("userLevel", order.getUserLevel());
        params.put("price", currentAmount);

        RuleExecuteResponse response = ruleEngine.executeRule("vip_discount", params);

        if (response.isSuccess() && response.getResult() instanceof Number) {
            BigDecimal result = new BigDecimal(response.getResult().toString());
            BigDecimal reduction = currentAmount.subtract(result);

            steps.add(new OrderProcessResult.ProcessStep(
                "VIP折扣",
                "根据用户等级 " + order.getUserLevel() + " 应用折扣",
                currentAmount,
                result,
                reduction,
                "vip_discount"
            ));

            log.info("VIP折扣规则执行结果: {} -> {}", currentAmount, result);
            return result;
        }

        // 折扣失败，添加无折扣步骤
        steps.add(new OrderProcessResult.ProcessStep(
            "VIP折扣",
            "VIP折扣规则执行失败，保持原价",
            currentAmount,
            currentAmount,
            BigDecimal.ZERO,
            "vip_discount"
        ));

        log.warn("VIP折扣规则执行失败，使用原价: {}", response.getErrorMessage());
        return currentAmount;
    }

    private BigDecimal applyFullReduction(Order order, BigDecimal currentAmount, List<OrderProcessResult.ProcessStep> steps) {
        Map<String, Object> params = new HashMap<>();
        params.put("totalAmount", currentAmount);
        params.put("userLevel", order.getUserLevel());

        RuleExecuteResponse response = ruleEngine.executeRule("full_reduction", params);

        if (response.isSuccess() && response.getResult() instanceof Number) {
            BigDecimal result = new BigDecimal(response.getResult().toString());
            BigDecimal reduction = currentAmount.subtract(result);

            String description = "满减活动";
            if (reduction.compareTo(BigDecimal.ZERO) > 0) {
                description += " (减免 ¥" + reduction + ")";
            } else {
                description += " (未满足条件)";
            }

            steps.add(new OrderProcessResult.ProcessStep(
                "满减活动",
                description,
                currentAmount,
                result,
                reduction,
                "full_reduction"
            ));

            log.info("满减规则执行结果: {} -> {}", currentAmount, result);
            return result;
        }

        // 满减失败，添加无减免步骤
        steps.add(new OrderProcessResult.ProcessStep(
            "满减活动",
            "满减规则执行失败，保持价格不变",
            currentAmount,
            currentAmount,
            BigDecimal.ZERO,
            "full_reduction"
        ));

        log.warn("满减规则执行失败，保持价格不变: {}", response.getErrorMessage());
        return currentAmount;
    }

    private Integer calculatePoints(BigDecimal finalAmount, Order order) {
        Map<String, Object> params = new HashMap<>();
        params.put("totalAmount", finalAmount);
        params.put("userLevel", order.getUserLevel());

        RuleExecuteResponse response = ruleEngine.executeRule("points_reward", params);

        if (response.isSuccess() && response.getResult() instanceof Number) {
            Integer points = ((Number) response.getResult()).intValue();
            log.info("积分奖励规则执行结果: {} -> {} 积分", finalAmount, points);
            return points;
        }

        log.warn("积分奖励规则执行失败，默认无积分: {}", response.getErrorMessage());
        return 0;
    }

    public Order createSampleOrder(String userLevel, BigDecimal amount) {
        Order order = new Order();
        order.setOrderId("ORDER-" + System.currentTimeMillis());
        order.setUserId("USER-123");
        order.setUserLevel(userLevel);
        order.setOriginalAmount(amount);
        order.setCreateTime(LocalDateTime.now());
        order.setStatus("CREATED");

        return order;
    }
}