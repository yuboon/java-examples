package com.example.pipeline.pipeline;

/**
 * 节点失败策略枚举
 */
public enum FailureStrategy {

    /**
     * 失败后继续执行下一个节点
     * 适用于：日志、通知等非关键操作
     */
    CONTINUE,

    /**
     * 失败后中断管道执行
     * 适用于：参数校验、权限校验等关键操作
     */
    STOP,

    /**
     * 失败后跳过当前节点，继续执行
     * 适用于：可选的操作
     */
    SKIP
}
