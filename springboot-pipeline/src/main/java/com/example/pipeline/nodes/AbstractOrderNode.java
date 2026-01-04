package com.example.pipeline.nodes;

import com.example.pipeline.model.Order;
import com.example.pipeline.model.OrderRequest;
import com.example.pipeline.pipeline.PipelineContext;
import com.example.pipeline.pipeline.PipelineNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单节点抽象基类
 * 提供通用方法和属性
 */
@Slf4j
public abstract class AbstractOrderNode implements PipelineNode<OrderRequest> {

    /**
     * 从上下文中获取订单
     */
    protected Order getOrder(PipelineContext<OrderRequest> context) {
        return context.getAttribute("ORDER");
    }

    /**
     * 将订单放入上下文
     */
    protected void setOrder(PipelineContext<OrderRequest> context, Order order) {
        context.setAttribute("ORDER", order);
    }

    /**
     * 从上下文中获取订单请求
     */
    protected OrderRequest getRequest(PipelineContext<OrderRequest> context) {
        return context.getData();
    }
}
