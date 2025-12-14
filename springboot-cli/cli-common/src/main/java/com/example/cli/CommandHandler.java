package com.example.cli;

/**
 * 统一命令处理接口
 * 所有需要通过CLI调用的服务都必须实现此接口
 */
public interface CommandHandler {

    /**
     * 处理CLI命令
     * @param args 命令参数数组
     * @return 命令执行结果
     */
    String handle(String[] args);

    /**
     * 获取命令描述信息
     * @return 命令描述
     */
    default String getDescription() {
        return "No description available";
    }

    /**
     * 获取命令使用帮助
     * @return 帮助信息
     */
    default String getUsage() {
        return "Usage: command [args...]";
    }
}