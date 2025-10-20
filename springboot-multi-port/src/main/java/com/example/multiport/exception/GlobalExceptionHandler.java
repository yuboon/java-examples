package com.example.multiport.exception;

import com.example.multiport.model.ErrorResponse;
import com.example.multiport.service.PortService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 根据不同端口提供不同的错误响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private PortService portService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {

        int port = request.getLocalPort();
        String serviceType = portService.getServiceType(port);
        String path = request.getRequestURI();

        log.error("全局异常处理 - 端口: {}, 服务类型: {}, 路径: {}, 异常: {}",
                port, serviceType, path, e.getMessage(), e);

        ErrorResponse error = new ErrorResponse();

        if (portService.isUserPort(port)) {
            // 用户端错误响应
            error.setCode("USER_ERROR_" + Math.abs(e.hashCode() % 1000));
            error.setMessage("用户服务暂时繁忙，请稍后重试");
            error.setServiceType("USER");
            error.setPort(portService.getUserPort());
        } else if (portService.isAdminPort(port)) {
            // 管理端错误响应
            error.setCode("ADMIN_ERROR_" + Math.abs(e.hashCode() % 1000));
            error.setMessage("管理服务异常: " + e.getMessage());
            error.setServiceType("ADMIN");
            error.setPort(portService.getAdminPort());
        }

        error.setPath(path);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException e, HttpServletRequest request) {

        int port = request.getLocalPort();
        String serviceType = portService.getServiceType(port);
        String path = request.getRequestURI();

        log.warn("404错误 - 端口: {}, 服务类型: {}, 路径: {}", port, serviceType, path);

        ErrorResponse error = new ErrorResponse();

        if (portService.isUserPort(port)) {
            error.setCode("USER_NOT_FOUND");
            error.setMessage("用户端接口不存在: " + path);
            error.setServiceType("USER");
            error.setPort(portService.getUserPort());
        } else if (portService.isAdminPort(port)) {
            error.setCode("ADMIN_NOT_FOUND");
            error.setMessage("管理端接口不存在: " + path);
            error.setServiceType("ADMIN");
            error.setPort(portService.getAdminPort());
        }

        error.setPath(path);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {

        int port = request.getLocalPort();
        String serviceType = portService.getServiceType(port);
        String path = request.getRequestURI();

        log.warn("参数错误 - 端口: {}, 服务类型: {}, 路径: {}, 错误: {}",
                port, serviceType, path, e.getMessage());

        ErrorResponse error = new ErrorResponse();

        if (portService.isUserPort(port)) {
            error.setCode("USER_BAD_REQUEST");
            error.setMessage("请求参数错误: " + e.getMessage());
            error.setServiceType("USER");
            error.setPort(portService.getUserPort());
        } else if (portService.isAdminPort(port)) {
            error.setCode("ADMIN_BAD_REQUEST");
            error.setMessage("请求参数错误: " + e.getMessage());
            error.setServiceType("ADMIN");
            error.setPort(portService.getAdminPort());
        }

        error.setPath(path);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

  }