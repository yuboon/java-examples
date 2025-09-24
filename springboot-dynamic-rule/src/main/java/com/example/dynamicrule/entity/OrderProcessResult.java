package com.example.dynamicrule.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcessResult {

    private String orderId;
    private String userId;
    private String userLevel;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private Integer pointsEarned;
    private LocalDateTime createTime;
    private String status;
    private List<ProcessStep> processSteps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessStep {
        private String stepName;
        private String description;
        private BigDecimal beforeAmount;
        private BigDecimal afterAmount;
        private BigDecimal reduction;
        private String ruleApplied;
    }
}