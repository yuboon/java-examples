package com.example.pipeline.nodes;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 创建订单节点
 * 核心业务节点，创建订单记录
 */
@Slf4j
@Component
public class CreateOrderNode extends AbstractOrderNode {

    // 模拟订单ID生成器
    private static final AtomicLong ORDER_ID_GENERATOR = new AtomicLong(1000);

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        OrderRequest request = getRequest(context);

        // 构建订单对象
        Order order = Order.builder()
                .id(ORDER_ID_GENERATOR.incrementAndGet())
                .orderNo(Order.generateOrderNo())
                .userId(request.getUserId())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(request.getTotalAmount())
                .address(request.getAddress())
                .remark(request.getRemark())
                .source(request.getSource())
                .status(Order.OrderStatus.PENDING)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 将订单放入上下文
        setOrder(context, order);

        // 更新用户订单计数
        BusinessValidateNode.USER_ORDER_COUNT
                .computeIfAbsent(request.getUserId(), k -> new AtomicInteger(0))
                .incrementAndGet();

        log.info("订单创建成功: orderId={}, orderNo={}, userId={}, amount={}",
                order.getId(), order.getOrderNo(), order.getUserId(), order.getTotalAmount());
    }
}
