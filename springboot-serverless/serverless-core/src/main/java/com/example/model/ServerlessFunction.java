package com.example.model;

import java.util.Map;

/**
 * Serverless函数接口
 * 所有用户函数都需要实现这个接口
 */
@FunctionalInterface
public interface ServerlessFunction {
    
    /**
     * 函数执行入口
     * @param input 输入参数
     * @param context 执行上下文
     * @return 执行结果
     */
    Object handle(Map<String, Object> input, ExecutionContext context) throws Exception;
}