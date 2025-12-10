package com.example.login.interceptor;

import com.example.login.model.ApiResponse;
import com.example.login.model.TokenInfo;
import com.example.login.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;

/**
 * 登录验证拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionManager sessionManager;

    @Value("${app.login.token-header:Authorization}")
    private String tokenHeader;

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        // 获取Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return handleUnauthorized(response, "请先登录");
        }

        // 验证Token
        TokenInfo tokenInfo = sessionManager.validateToken(token);
        if (tokenInfo == null) {
            return handleUnauthorized(response, "登录已过期，请重新登录");
        }

        // 将Token信息存入请求属性
        request.setAttribute("tokenInfo", tokenInfo);
        request.setAttribute("username", tokenInfo.getUsername());

        return true;
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从Header中获取
        String token = request.getHeader(tokenHeader);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        return null;
    }

    /**
     * 处理未授权请求
     */
    private boolean handleUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);

        ApiResponse<Void> result = ApiResponse.fail(401, message);
        response.getWriter().write(
            new ObjectMapper().writeValueAsString(result)
        );

        return false;
    }
}