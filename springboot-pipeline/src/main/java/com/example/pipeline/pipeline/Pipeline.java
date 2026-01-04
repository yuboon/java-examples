package com.example.pipeline.pipeline;

/**
 * 管道接口
 *
 * @param <T> 数据类型
 */
public interface Pipeline<T> {

    /**
     * 执行管道
     *
     * @param data 输入数据
     * @return 执行结果上下文
     */
    PipelineContext<T> execute(T data);

    /**
     * 创建管道构建器
     *
     * @param <T> 数据类型
     * @return 构建器
     */
    static <T> PipelineBuilder<T> builder() {
        return new PipelineBuilder<>();
    }
}
