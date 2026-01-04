package com.example.pipeline.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单创建响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /**
     * 订单信息
     */
    private Order order;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行的节点列表
     */
    private List<String> executedNodes;

    /**
     * 失败的节点列表
     */
    private List<FailureInfo> failures;

    /**
     * 失败节点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureInfo {
        private String nodeName;
        private String reason;
        private Long timestamp;
    }
}
