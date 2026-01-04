package com.example.pipeline.pipeline;

/**
 * 管道节点接口
 * 每个节点只做一件事，不关心前后节点是谁
 *
 * @param <T> 上下文数据类型
 */
public interface PipelineNode<T> {

    /**
     * 执行节点逻辑
     *
     * @param context 管道上下文
     * @throws PipelineException 节点执行异常
     */
    void execute(PipelineContext<T> context) throws PipelineException;

    /**
     * 获取节点名称
     *
     * @return 节点名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取节点失败策略
     *
     * @return 失败策略
     */
    default FailureStrategy getFailureStrategy() {
        return FailureStrategy.STOP;
    }
}
