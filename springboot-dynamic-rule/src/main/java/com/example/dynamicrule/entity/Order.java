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
public class Order {

    private String orderId;

    private String userId;

    private String userLevel;

    private BigDecimal originalAmount;

    private BigDecimal finalAmount;

    private Integer pointsEarned;

    private List<OrderItem> items;

    private LocalDateTime createTime;

    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private String category;
    }
}