package com.example.pipeline.nodes;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.FailureStrategy;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 操作日志节点
 * 记录订单创建日志（失败不影响主流程）
 */
@Slf4j
@Component
public class OperateLogNode extends AbstractOrderNode {

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        Order order = getOrder(context);

        if (order == null) {
            log.warn("订单不存在，跳过日志记录");
            return;
        }

        try {
            // 模拟写日志
            log.info("=== 操作日志 ===");
            log.info("操作类型: CREATE");
            log.info("订单ID: {}", order.getId());
            log.info("订单号: {}", order.getOrderNo());
            log.info("用户ID: {}", order.getUserId());
            log.info("商品: {} (ID: {})", order.getProductName(), order.getProductId());
            log.info("数量: {}", order.getQuantity());
            log.info("单价: {}", order.getUnitPrice());
            log.info("总金额: {}", order.getTotalAmount());
            log.info("收货地址: {}", order.getAddress());
            log.info("订单来源: {}", order.getSource());
            log.info("创建时间: {}", order.getCreateTime());
            log.info("===============");

        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("日志记录失败", e);
            throw new PipelineException(getName(), "日志记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FailureStrategy getFailureStrategy() {
        return FailureStrategy.CONTINUE;
    }
}
