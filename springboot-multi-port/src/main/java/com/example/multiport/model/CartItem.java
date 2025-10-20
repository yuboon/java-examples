package com.example.multiport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private Long id;

    private Long userId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private BigDecimal price;

    private LocalDateTime addTime;
}