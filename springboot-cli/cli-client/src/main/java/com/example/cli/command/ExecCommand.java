package com.example.cli.command;

import cn.hutool.http.HttpUtil;
import com.example.cli.CliClientProperties;
import com.example.cli.dto.CommandRequest;
import com.example.cli.dto.CommandResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 通用CLI命令 (Executive Command Service)
 */
@ShellComponent
@Component
public class ExecCommand {

    private static final Logger logger = LoggerFactory.getLogger(ExecCommand.class);

    @Autowired
    private CliClientProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行远程服务命令
     * @param serviceName 服务名称
     * @return 执行结果
     */
    @ShellMethod(key = "exec", value = "执行远程服务命令。用法: exec <serviceName> --args arg1 arg2 ...")
    public String executeCommand(
            @ShellOption(value = {"", "service"}, help = "服务名称") String serviceName,
            @ShellOption(value = "--args", help = "命令参数", arity = 100) String[] args) {

        if (serviceName == null || serviceName.trim().isEmpty()) {
            return "错误：服务名称不能为空\n用法: exec <serviceName> [--args arg1 arg2 ...]";
        }

        // 处理 null args
        if (args == null) {
            args = new String[0];
        }

        try {
            // 构建请求
            CommandRequest request = new CommandRequest(serviceName.trim(),
                    Arrays.asList(args));

            // 发送请求
            String response = HttpUtil.post(properties.getServerUrl() + "/cli", objectMapper.writeValueAsString(request));

            // 解析响应
            CommandResponse commandResponse = objectMapper.readValue(
                    response, new TypeReference<CommandResponse>() {});

            if (commandResponse.isSuccess()) {
                return formatResponse(commandResponse.getData());
            } else {
                return "错误: " + commandResponse.getMessage();
            }

        } catch (Exception e) {
            logger.error("命令执行失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 列出所有可用的服务
     */
    @ShellMethod(key = "list-services", value = "列出所有可用的服务")
    public String listServices() {
        try {
           /* String response = webClient.get()
                    .uri(properties.getServerUrl() + "/cli/services")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.getTimeout()))
                    .block();
                    .block();
            */

            String response = HttpUtil.post(properties.getServerUrl() + "/cli/services", new HashMap<>());

            ObjectMapper mapper = new ObjectMapper();
            Object result = mapper.readValue(response, Object.class);

            return "可用服务列表:\n" +
                   mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        } catch (Exception e) {
            logger.error("获取服务列表失败", e);
            return "获取服务列表失败: " + e.getMessage();
        }
    }

    /**
     * 显示帮助信息
     */
    @ShellMethod(key = "help-exec", value = "显示EXEC命令帮助")
    public String help() {
        return """
                通用命令服务 (EXEC) 使用说明:

                基本命令:
                  exec <serviceName> --args [arg1 arg2 ...]  - 执行远程服务命令
                  list-services                              - 列出所有可用服务
                  help-exec                                  - 显示此帮助信息

                示例:
                  exec userService --args list               - 获取用户列表
                  exec userService --args get 1              - 获取ID为1的用户
                  exec roleService --args users admin        - 获取管理员角色列表
                  exec systemService --args status           - 获取系统状态

                配置:
                  服务器地址: """ + properties.getServerUrl() + "\n" +
                  "超时时间: " + properties.getTimeout() + "ms\n";
    }

    /**
     * 格式化响应输出
     */
    private String formatResponse(String data) {
        if (data == null) {
            return "命令执行成功，无返回数据";
        }

        // 如果是JSON格式，尝试美化输出
        try {
            Object json = objectMapper.readValue(data, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            // 不是JSON格式，直接返回
            return data;
        }
    }
}