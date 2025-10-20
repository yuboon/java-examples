package com.example.multiport.interceptor;

import com.example.multiport.service.PortService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 端口感知拦截器
 * 根据不同端口进行不同的请求处理逻辑
 */
@Slf4j
@Component
public class PortAwareInterceptor implements HandlerInterceptor {

    @Autowired
    private PortService portService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        int port = request.getLocalPort();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 只记录API请求，避免静态资源日志干扰
        if (uri.startsWith("/api/")) {
            log.info("端口感知拦截器 - 端口: {}, 方法: {}, URI: {}", port, method, uri);
        }

        if (portService.isUserPort(port)) {
            // 用户端逻辑
            return handleUserRequest(request, response);
        } else if (portService.isAdminPort(port)) {
            // 管理端逻辑
            return handleAdminRequest(request, response);
        }

        return true;
    }

    /**
     * 处理用户端请求
     */
    private boolean handleUserRequest(HttpServletRequest request, HttpServletResponse response) {
        // 用户端请求验证逻辑
        String userAgent = request.getHeader("User-Agent");

        // 可以根据需要添加用户端特定的验证逻辑
        if (userAgent != null && userAgent.contains("bot")) {
            log.warn("检测到可能的机器人访问用户端: {}", userAgent);
            // 可以选择拒绝或记录机器人访问
        }

        // 设置用户端响应头
        response.setHeader("X-Service-Type", "USER");
        response.setHeader("X-Port", portService.getUserPortString());

        return true;
    }

    /**
     * 处理管理端请求
     */
    private boolean handleAdminRequest(HttpServletRequest request, HttpServletResponse response) {
        // 管理端请求处理逻辑（已移除认证检查）
        log.info("管理端请求处理: {}", request.getRequestURI());

        // 设置管理端响应头
        response.setHeader("X-Service-Type", "ADMIN");
        response.setHeader("X-Port", portService.getAdminPortString());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) throws Exception {

        int port = request.getLocalPort();
        String serviceType = portService.getServiceType(port);
        String uri = request.getRequestURI();

        // 只记录API请求的完成状态
        if (uri.startsWith("/api/")) {
            if (ex != null) {
                log.error("{}端口请求处理完成，但发生异常: {}", serviceType, ex.getMessage());
            } else {
                log.debug("{}端口请求处理完成，状态码: {}", serviceType, response.getStatus());
            }
        }
    }
}