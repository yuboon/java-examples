package com.example.pipeline.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 管道构建器
 * 使用 Builder 模式构建管道
 *
 * @param <T> 数据类型
 */
public class PipelineBuilder<T> {

    private final List<PipelineNode<T>> nodes;
    private String name = "DefaultPipeline";

    public PipelineBuilder() {
        this.nodes = new ArrayList<>();
    }

    /**
     * 添加节点
     *
     * @param node 节点
     * @return this
     */
    public PipelineBuilder<T> add(PipelineNode<T> node) {
        this.nodes.add(node);
        return this;
    }

    /**
     * 添加多个节点
     *
     * @param newNodes 节点列表
     * @return this
     */
    public PipelineBuilder<T> addAll(List<PipelineNode<T>> newNodes) {
        this.nodes.addAll(newNodes);
        return this;
    }

    /**
     * 设置管道名称
     *
     * @param name 名称
     * @return this
     */
    public PipelineBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 条件添加节点
     *
     * @param condition 条件
     * @param node      节点
     * @return this
     */
    public PipelineBuilder<T> addIf(boolean condition, PipelineNode<T> node) {
        if (condition) {
            this.nodes.add(node);
        }
        return this;
    }

    /**
     * 构建管道
     *
     * @return 管道实例
     */
    public Pipeline<T> build() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Pipeline must have at least one node");
        }
        return new ExecutionPipeline<>(nodes, name);
    }
}
