package com.example.pipeline.nodes;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.FailureStrategy;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步风控检查节点
 * 异步执行风控检查，不阻塞主流程
 */
@Slf4j
@Component
public class AsyncRiskCheckNode extends AbstractOrderNode {

    // 模拟风控黑名单用户
    private static final Set<Long> RISK_USERS = Set.of(777L);

    // 风控检查结果缓存
    private static final ConcurrentHashMap<Long, RiskCheckResult> RISK_RESULT_CACHE = new ConcurrentHashMap<>();

    private final Random random = new Random();

    @Override
    public void execute(PipelineContext<OrderRequest> context) throws PipelineException {
        Order order = getOrder(context);

        if (order == null) {
            log.warn("订单不存在，跳过风控检查");
            return;
        }

        OrderRequest request = getRequest(context);

        // 测试环境下可以跳过风控
        if (Boolean.TRUE.equals(request.getSkipRiskCheck())) {
            log.info("测试环境，跳过风控检查: orderId={}", order.getId());
            return;
        }

        try {
            // 异步执行风控检查
            final Long orderId = order.getId();
            CompletableFuture.runAsync(() -> performRiskCheck(order))
                    .exceptionally(e -> {
                        log.error("风控检查异步执行失败: orderId={}", orderId, e);
                        return null;
                    });

            log.info("风控检查已提交异步执行: orderId={}", order.getId());

        } catch (Exception e) {
            log.error("风控检查提交失败", e);
            throw new PipelineException(getName(), "风控检查提交失败: " + e.getMessage(), e);
        }
    }

    private void performRiskCheck(Order order) {
        try {
            // 模拟风控检查耗时
            Thread.sleep(500 + random.nextInt(1000));

            Long userId = order.getUserId();

            // 检查是否命中风控规则
            RiskCheckResult result = checkRiskRules(order);

            RISK_RESULT_CACHE.put(order.getId(), result);

            if (result.isRisky()) {
                log.warn("风控检查发现异常: orderId={}, userId={}, riskReason={}",
                        order.getId(), userId, result.getReason());
            } else {
                log.info("风控检查通过: orderId={}, userId={}", order.getId(), userId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("风控检查被中断: orderId={}", order.getId(), e);
        }
    }

    private RiskCheckResult checkRiskRules(Order order) {
        // 规则1: 检查用户是否在风控黑名单
        if (RISK_USERS.contains(order.getUserId())) {
            return new RiskCheckResult(true, "用户在风控黑名单中");
        }

        // 规则2: 检查订单金额是否异常
        if (order.getTotalAmount().compareTo(new java.math.BigDecimal("10000")) > 0) {
            return new RiskCheckResult(true, "订单金额异常");
        }

        // 规则3: 检查是否为恶意刷单（模拟）
        if (order.getQuantity() > 100) {
            return new RiskCheckResult(true, "订单数量异常");
        }

        // 风控检查通过
        return new RiskCheckResult(false, "正常");
    }

    @Override
    public FailureStrategy getFailureStrategy() {
        return FailureStrategy.CONTINUE;
    }

    /**
     * 获取风控检查结果
     */
    public static RiskCheckResult getRiskCheckResult(Long orderId) {
        return RISK_RESULT_CACHE.get(orderId);
    }

    /**
     * 清除风控检查结果
     */
    public static void clearRiskCheckResult(Long orderId) {
        RISK_RESULT_CACHE.remove(orderId);
    }

    /**
     * 风控检查结果
     */
    public static class RiskCheckResult {
        private final boolean risky;
        private final String reason;

        public RiskCheckResult(boolean risky, String reason) {
            this.risky = risky;
            this.reason = reason;
        }

        public boolean isRisky() {
            return risky;
        }

        public String getReason() {
            return reason;
        }
    }
}
