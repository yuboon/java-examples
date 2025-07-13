package com.example.unified.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象，表示订单基本 information 
 */
@Data  
public class OrderDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 显式声明版本号

    private Long id;                     // 订单ID
    private Long userId;                 // 用户ID
    private String totalAmount;      // 订单总金额
    private String status;               // 订单状态（PENDING/SUBMITTED/PAID）
    private LocalDateTime createTime;    // 创建时间
}