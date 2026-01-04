package com.example.pipeline.controller;

import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.model.OrderResponse;
import com.example.pipeline.service.OrderService;
import com.example.pipeline.nodes.AsyncRiskCheckNode;
import com.example.pipeline.nodes.BusinessValidateNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 订单请求
     * @return 订单响应
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("收到订单创建请求: userId={}, productId={}", request.getUserId(), request.getProductId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询风控检查结果
     *
     * @param orderId 订单ID
     * @return 风控检查结果
     */
    @GetMapping("/{orderId}/risk-check")
    public ResponseEntity<RiskCheckResponse> getRiskCheckResult(@PathVariable Long orderId) {
        AsyncRiskCheckNode.RiskCheckResult result = AsyncRiskCheckNode.getRiskCheckResult(orderId);

        if (result == null) {
            return ResponseEntity.ok(new RiskCheckResponse(false, "风控检查中或未执行", false));
        }

        return ResponseEntity.ok(new RiskCheckResponse(
                true,
                result.getReason(),
                result.isRisky()
        ));
    }

    /**
     * 重置用户订单计数（测试接口）
     *
     * @param userId 用户ID
     */
    @PostMapping("/test/reset-user-count/{userId}")
    public ResponseEntity<String> resetUserOrderCount(@PathVariable Long userId) {
        BusinessValidateNode.resetUserOrderCount(userId);
        return ResponseEntity.ok("用户订单计数已重置: userId=" + userId);
    }

    /**
     * 风控检查响应
     */
    public record RiskCheckResponse(
            boolean checked,
            String message,
            boolean risky
    ) {}
}
