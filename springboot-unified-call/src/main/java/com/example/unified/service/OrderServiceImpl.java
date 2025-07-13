package com.example.unified.service;

import com.example.unified.dto.OrderDTO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DubboService(version = "1.0.0")
public class OrderServiceImpl implements OrderService {

    public OrderDTO getOrder(OrderDTO orderDTO) {
        // 模拟数据库查询
        OrderDTO order = new OrderDTO();
        order.setId(orderDTO.getId());
        order.setUserId(1001L);
        order.setTotalAmount("99.99");
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

}