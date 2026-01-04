package com.example.pipeline.service;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.model.OrderResponse;
import com.example.pipeline.nodes.*;
import com.example.pipeline.pipeline.Pipeline;
import com.example.pipeline.pipeline.PipelineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单服务
 * 使用执行管道处理订单创建流程
 */
@Slf4j
@Service
public class OrderService {

    private final ParamValidateNode paramValidateNode;
    private final PermissionCheckNode permissionCheckNode;
    private final BusinessValidateNode businessValidateNode;
    private final CreateOrderNode createOrderNode;
    private final OperateLogNode operateLogNode;
    private final NotificationNode notificationNode;
    private final AsyncRiskCheckNode asyncRiskCheckNode;

    public OrderService(
            ParamValidateNode paramValidateNode,
            PermissionCheckNode permissionCheckNode,
            BusinessValidateNode businessValidateNode,
            CreateOrderNode createOrderNode,
            OperateLogNode operateLogNode,
            NotificationNode notificationNode,
            AsyncRiskCheckNode asyncRiskCheckNode) {
        this.paramValidateNode = paramValidateNode;
        this.permissionCheckNode = permissionCheckNode;
        this.businessValidateNode = businessValidateNode;
        this.createOrderNode = createOrderNode;
        this.operateLogNode = operateLogNode;
        this.notificationNode = notificationNode;
        this.asyncRiskCheckNode = asyncRiskCheckNode;
    }

    /**
     * 创建订单
     * 使用执行管道模式处理订单创建流程
     *
     * @param request 订单请求
     * @return 订单响应
     */
    public OrderResponse createOrder(OrderRequest request) {
        log.info("开始创建订单: userId={}, productId={}", request.getUserId(), request.getProductId());

        // 构建订单创建管道
        Pipeline<OrderRequest> pipeline = Pipeline.<OrderRequest>builder()
                .name("OrderCreationPipeline")
                .add(paramValidateNode)       // 1. 参数校验
                .add(permissionCheckNode)     // 2. 权限校验
                .add(businessValidateNode)    // 3. 业务校验
                .add(createOrderNode)         // 4. 创建订单
                .add(operateLogNode)          // 5. 记录日志
                .add(notificationNode)        // 6. 发送通知
                .add(asyncRiskCheckNode)      // 7. 风控检查（异步）
                .build();

        // 执行管道
        PipelineContext<OrderRequest> context = pipeline.execute(request);

        // 构建响应
        return buildResponse(context);
    }

    /**
     * 获取订单详情（从管道上下文中获取）
     */
    public Order getOrderFromContext(PipelineContext<OrderRequest> context) {
        return context.getAttribute("ORDER");
    }

    /**
     * 构建响应对象
     */
    private OrderResponse buildResponse(PipelineContext<OrderRequest> context) {
        Order order = context.getAttribute("ORDER");

        // 转换失败信息
        List<OrderResponse.FailureInfo> failureInfos = context.getFailures().stream()
                .map(f -> OrderResponse.FailureInfo.builder()
                        .nodeName(f.getNodeName())
                        .reason(f.getReason())
                        .timestamp(f.getTimestamp())
                        .build())
                .toList();

        boolean success = (order != null) && context.getFailures().isEmpty();

        return OrderResponse.builder()
                .order(order)
                .success(success)
                .errorMessage(success ? null : getErrorMessage(context))
                .executedNodes(context.getExecutedNodes())
                .failures(failureInfos)
                .build();
    }

    /**
     * 获取错误信息
     */
    private String getErrorMessage(PipelineContext<OrderRequest> context) {
        if (context.getInterruptReason() != null) {
            return context.getInterruptReason();
        }
        if (!context.getFailures().isEmpty()) {
            return context.getFailures().get(0).getReason();
        }
        return "未知错误";
    }
}
