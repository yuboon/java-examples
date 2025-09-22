package com.example.staticpermit.interceptor;

import com.example.staticpermit.service.FilePermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 方案三：静态资源权限拦截器
 * 专门拦截指向私有静态资源目录的请求，执行权限校验
 */
@Component
public class Solution3StaticResourceAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private FilePermissionService filePermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. 获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            // 用户未登录，返回401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 2. 从请求路径中解析出文件名
        String requestURI = request.getRequestURI(); // e.g., /uploads/private-file.txt
        String filename = requestURI.substring(requestURI.lastIndexOf("/") + 1);

        // 3. 执行权限检查
        if (!filePermissionService.hasPermission(authentication, filename)) {
            // 无权限，返回403
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // 4. 有权限，放行
        // preHandle返回true后，请求会继续流转到Spring默认的ResourceHttpRequestHandler，
        // 由它来完成静态文件的读取和响应
        return true;
    }
}