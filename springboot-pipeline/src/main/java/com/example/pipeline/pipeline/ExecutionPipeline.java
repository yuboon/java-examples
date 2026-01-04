package com.example.pipeline.pipeline;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 执行管道实现
 *
 * @param <T> 数据类型
 */
@Slf4j
public class ExecutionPipeline<T> implements Pipeline<T> {

    private final List<PipelineNode<T>> nodes;
    private final String name;

    ExecutionPipeline(List<PipelineNode<T>> nodes, String name) {
        this.nodes = nodes;
        this.name = name;
    }

    @Override
    public PipelineContext<T> execute(T data) {
        log.info("Pipeline [{}] started with {} nodes", name, nodes.size());

        PipelineContext<T> context = new PipelineContext<>(data);

        for (PipelineNode<T> node : nodes) {
            if (context.isInterrupted()) {
                log.info("Pipeline [{}] interrupted: {}", name, context.getInterruptReason());
                break;
            }

            executeNode(node, context);
        }

        log.info("Pipeline [{}] completed. Executed: {}, Failures: {}",
                name, context.getExecutedNodes().size(), context.getFailures().size());

        return context;
    }

    private void executeNode(PipelineNode<T> node, PipelineContext<T> context) {
        String nodeName = node.getName();
        log.debug("Executing node: {}", nodeName);

        try {
            node.execute(context);
            context.markNodeExecuted(nodeName);
            log.debug("Node [{}] executed successfully", nodeName);
        } catch (Exception e) {
            log.error("Node [{}] execution failed", nodeName, e);

            FailureStrategy strategy = node.getFailureStrategy();
            context.recordFailure(nodeName, e.getMessage(), e);

            switch (strategy) {
                case STOP:
                    context.interrupt("Node [" + nodeName + "] failed with STOP strategy");
                    break;
                case CONTINUE:
                case SKIP:
                    // 继续执行下一个节点
                    break;
            }
        }
    }
}
