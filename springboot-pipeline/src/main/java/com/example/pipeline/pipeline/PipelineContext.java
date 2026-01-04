package com.example.pipeline.pipeline;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管道上下文
 * 用于在节点之间传递数据和状态
 *
 * @param <T> 主要数据类型
 */
@Getter
public class PipelineContext<T> {

    /**
     * 主要业务数据
     */
    private final T data;

    /**
     * 是否中断管道执行
     */
    private boolean interrupted;

    /**
     * 中断原因
     */
    private String interruptReason;

    /**
     * 执行过的节点列表
     */
    private final List<String> executedNodes;

    /**
     * 失败的节点列表
     */
    private final List<NodeFailure> failures;

    /**
     * 扩展属性，用于节点间传递额外数据
     */
    private final Map<String, Object> attributes;

    public PipelineContext(T data) {
        this.data = data;
        this.executedNodes = new ArrayList<>();
        this.failures = new ArrayList<>();
        this.attributes = new HashMap<>();
        this.interrupted = false;
    }

    /**
     * 中断管道执行
     */
    public void interrupt(String reason) {
        this.interrupted = true;
        this.interruptReason = reason;
    }

    /**
     * 标记节点执行完成
     */
    public void markNodeExecuted(String nodeName) {
        this.executedNodes.add(nodeName);
    }

    /**
     * 记录节点失败
     */
    public void recordFailure(String nodeName, String reason, Throwable cause) {
        this.failures.add(new NodeFailure(nodeName, reason, cause));
    }

    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key) {
        return (V) this.attributes.get(key);
    }

    /**
     * 获取扩展属性，支持默认值
     */
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key, V defaultValue) {
        return (V) this.attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 节点失败记录
     */
    @Getter
    public static class NodeFailure {
        private final String nodeName;
        private final String reason;
        private final Throwable cause;
        private final long timestamp;

        public NodeFailure(String nodeName, String reason, Throwable cause) {
            this.nodeName = nodeName;
            this.reason = reason;
            this.cause = cause;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
