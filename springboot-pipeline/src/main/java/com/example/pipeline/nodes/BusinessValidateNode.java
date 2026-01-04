package com.example.pipeline.nodes;

import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务校验节点
 * 检查业务规则是否满足
 */
@Slf4j
@Component
public class BusinessValidateNode extends AbstractOrderNode {

    // 模拟用户订单计数
    public static final ConcurrentHashMap<Long, AtomicInteger> USER_ORDER_COUNT = new ConcurrentHashMap<>();

    // 每个用户最大订单数量
    private static final int MAX_ORDERS_PER_USER = 10;

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        OrderRequest request = getRequest(context);
        Long userId = request.getUserId();

        // 检查用户订单数量限制
        AtomicInteger count = USER_ORDER_COUNT.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int currentCount = count.get();

        if (currentCount >= MAX_ORDERS_PER_USER) {
            throw new PipelineException(getName(),
                    String.format("用户订单数量已达上限 (%d/%d)", currentCount, MAX_ORDERS_PER_USER));
        }

        // 检查商品库存（模拟）
        if (request.getProductId() == 1001) {
            throw new PipelineException(getName(), "商品已售罄");
        }

        // 检查收货地址格式（模拟）
        if (request.getAddress().length() < 5) {
            throw new PipelineException(getName(), "收货地址格式不正确");
        }

        log.info("业务校验通过: userId={}, productId={}, currentOrderCount={}",
                userId, request.getProductId(), currentCount);
    }

    /**
     * 重置用户订单计数（测试用）
     */
    public static void resetUserOrderCount(Long userId) {
        USER_ORDER_COUNT.remove(userId);
    }
}
