package com.example.pipeline.nodes;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.FailureStrategy;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通知节点
 * 发送订单创建通知（失败不影响主流程）
 */
@Slf4j
@Component
public class NotificationNode extends AbstractOrderNode {

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        Order order = getOrder(context);

        if (order == null) {
            log.warn("订单不存在，跳过通知发送");
            return;
        }

        try {
            // 模拟发送短信通知
            sendSmsNotification(order);

            // 模拟发送邮件通知
            sendEmailNotification(order);

            // 模拟推送通知
            sendPushNotification(order);

            log.info("订单通知发送成功: orderId={}, userId={}", order.getId(), order.getUserId());

        } catch (Exception e) {
            log.error("通知发送失败", e);
            throw new PipelineException(getName(), "通知发送失败: " + e.getMessage(), e);
        }
    }

    private void sendSmsNotification(Order order) {
        log.info("【订单提醒】您已成功创建订单，订单号: {}，金额: {}元",
                order.getOrderNo(), order.getTotalAmount());
    }

    private void sendEmailNotification(Order order) {
        log.info("发送邮件给用户 {}: 订单 {} 创建成功", order.getUserId(), order.getOrderNo());
    }

    private void sendPushNotification(Order order) {
        log.info("推送通知: 订单 {} 创建成功", order.getOrderNo());
    }

    @Override
    public FailureStrategy getFailureStrategy() {
        return FailureStrategy.CONTINUE;
    }
}
