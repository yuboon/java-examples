package com.example.cli.controller;

import com.example.cli.CliProperties;
import com.example.cli.CommandHandler;
import com.example.cli.dto.CommandRequest;
import com.example.cli.dto.CommandResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CLI统一命令接口控制器
 */
@RestController
@RequestMapping("/cli")
@Validated
@EnableConfigurationProperties(CliProperties.class)
public class CliController {

    private static final Logger logger = LoggerFactory.getLogger(CliController.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CliProperties cliProperties;

    private final Set<String> allowedServices = new HashSet<>();

    /**
     * 初始化允许访问的服务列表
     */
    private void initializeAllowedServices() {
        if (allowedServices.isEmpty()) {
            allowedServices.addAll(cliProperties.getAllowedServices());
        }
    }

    /**
     * 执行CLI命令
     */
    @PostMapping
    public ResponseEntity<CommandResponse> execute(
            @RequestBody CommandRequest request,
            HttpServletRequest httpRequest) {

        initializeAllowedServices();

        String serviceName = request.getService();
        String[] args = request.getArgs() != null ?
                request.getArgs().toArray(new String[0]) : new String[0];

        logger.info("CLI请求 - 服务: {}, 参数: {}, 来源: {}",
                serviceName, Arrays.toString(args), httpRequest.getRemoteAddr());

        // 检查服务是否在白名单中
        if (!allowedServices.isEmpty() && !allowedServices.contains(serviceName)) {
            logger.warn("未授权的服务访问: {}", serviceName);
            return ResponseEntity.ok(CommandResponse.error("未授权的服务: " + serviceName));
        }

        // 获取Service Bean
        Object serviceBean;
        try {
            serviceBean = applicationContext.getBean(serviceName);
        } catch (NoSuchBeanDefinitionException e) {
            logger.warn("服务不存在: {}", serviceName);
            return ResponseEntity.ok(CommandResponse.error("服务不存在: " + serviceName));
        }

        // 检查是否实现了CommandHandler接口
        if (!(serviceBean instanceof CommandHandler handler)) {
            logger.warn("服务未实现CommandHandler接口: {}", serviceName);
            return ResponseEntity.ok(CommandResponse.error("服务未实现CommandHandler接口: " + serviceName));
        }

        try {
            // 执行命令
            String result = handler.handle(args);
            logger.info("命令执行成功 - 服务: {}", serviceName);
            return ResponseEntity.ok(CommandResponse.success(result));
        } catch (Exception e) {
            logger.error("命令执行失败 - 服务: " + serviceName, e);
            return ResponseEntity.ok(CommandResponse.error("命令执行失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有可用的服务列表
     */
    @GetMapping("/services")
    public ResponseEntity<Object> getServices() {
        initializeAllowedServices();

        return ResponseEntity.ok(new Object() {
            public final Set<String> availableServices = allowedServices;
            public final LocalDateTime timestamp = LocalDateTime.now();
        });
    }
}