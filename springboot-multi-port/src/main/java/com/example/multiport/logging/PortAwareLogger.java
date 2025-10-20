package com.example.multiport.logging;

import com.example.multiport.service.PortService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 端口感知日志记录器
 * 根据不同端口使用不同的日志记录策略
 */
@Component
@Slf4j
public class PortAwareLogger {

    @Autowired
    private PortService portService;

    private static final Logger userLogger = LoggerFactory.getLogger("USER-PORT");
    private static final Logger adminLogger = LoggerFactory.getLogger("ADMIN-PORT");
    private static final Logger systemLogger = LoggerFactory.getLogger("SYSTEM");

    /**
     * 记录请求信息
     */
    public void logRequest(HttpServletRequest request) {
        int port = request.getLocalPort();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);

        if (portService.isUserPort(port)) {
            userLogger.info("用户端请求 - IP: {}, 方法: {}, URI: {}, UserAgent: {}",
                    clientIp, method, uri, userAgent);
        } else if (portService.isAdminPort(port)) {
            adminLogger.info("管理端请求 - IP: {}, 方法: {}, URI: {}, UserAgent: {}",
                    clientIp, method, uri, userAgent);
        }
    }

    /**
     * 记录响应信息
     */
    public void logResponse(HttpServletRequest request, int status, long duration) {
        int port = request.getLocalPort();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (portService.isUserPort(port)) {
            userLogger.info("用户端响应 - 方法: {}, URI: {}, 状态码: {}, 耗时: {}ms",
                    method, uri, status, duration);
        } else if (portService.isAdminPort(port)) {
            adminLogger.info("管理端响应 - 方法: {}, URI: {}, 状态码: {}, 耗时: {}ms",
                    method, uri, status, duration);
        }
    }

    /**
     * 记录业务操作
     */
    public void logBusinessOperation(int port, String operation, String details) {
        if (portService.isUserPort(port)) {
            userLogger.info("用户端业务操作 - 操作: {}, 详情: {}", operation, details);
        } else if (portService.isAdminPort(port)) {
            adminLogger.info("管理端业务操作 - 操作: {}, 详情: {}", operation, details);
        }
    }

    /**
     * 记录安全事件
     */
    public void logSecurityEvent(int port, String event, String details) {
        String serviceType = portService.getServiceTypeChinese(port);
        systemLogger.warn("安全事件 - 服务类型: {}, 事件: {}, 详情: {}", serviceType, event, details);
    }

    /**
     * 记录性能指标
     */
    public void logPerformanceMetrics(int port, String endpoint, long responseTime) {
        if (portService.isUserPort(port)) {
            userLogger.info("性能指标 - 端点: {}, 响应时间: {}ms", endpoint, responseTime);
        } else if (portService.isAdminPort(port)) {
            adminLogger.info("性能指标 - 端点: {}, 响应时间: {}ms", endpoint, responseTime);
        }
    }

    /**
     * 记录错误信息
     */
    public void logError(int port, String error, Throwable throwable) {
        if (portService.isUserPort(port)) {
            userLogger.error("用户端错误 - 错误: {}", error, throwable);
        } else if (portService.isAdminPort(port)) {
            adminLogger.error("管理端错误 - 错误: {}", error, throwable);
        }
    }

    /**
     * 记录系统启动信息
     */
    public void logSystemStartup() {
        systemLogger.info("多端口应用启动完成 - 用户端端口: {}, 管理端端口: {}",
                portService.getUserPort(), portService.getAdminPort());
    }

    /**
     * 记录端口状态变化
     */
    public void logPortStatusChange(int port, String status) {
        String serviceType = portService.getServiceTypeChinese(port);
        systemLogger.info("端口状态变化 - 服务类型: {}, 端口: {}, 状态: {}", serviceType, port, status);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}