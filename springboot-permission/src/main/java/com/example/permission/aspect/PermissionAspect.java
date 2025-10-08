package com.example.permission.aspect;

import com.example.permission.annotation.CheckPermission;
import com.example.permission.common.AccessDeniedException;
import com.example.permission.entity.Document;
import com.example.permission.entity.User;
import com.example.permission.service.DocumentService;
import com.example.permission.service.EnforcerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限检查切面
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private EnforcerService enforcerService;

    @Autowired
    private DocumentService documentService;

    @Before("@annotation(checkPermission)")
    public void checkAuth(JoinPoint joinPoint, CheckPermission checkPermission) {
        log.debug("权限检查开始：action={}", checkPermission.action());

        // 1. 获取当前用户
        User user = getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("未登录");
        }

        // 2. 获取资源对象
        Document resource = getResource(joinPoint, checkPermission);
        if (resource == null) {
            throw new AccessDeniedException("资源对象不存在");
        }

        // 3. 执行权限检查
        String action = checkPermission.action();
        boolean allowed = enforcerService.enforce(user, resource, action);

        log.info("权限检查结果：user={}, resource={}, action={}, allowed={}",
                user.getId(), resource.getId(), action, allowed);

        if (!allowed) {
            throw new AccessDeniedException(
                    String.format("无权限执行操作：%s (用户=%s, 资源=%s)", action, user.getId(), resource.getId())
            );
        }
    }

    /**
     * 从请求上下文获取当前用户（简化版，实际应从 Session/JWT 获取）
     */
    private User getCurrentUser() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();

        // 从 Header 中获取用户信息（演示用）
        String userId = request.getHeader("X-User-Id");
        String userName = request.getHeader("X-User-Name");
        String userDept = request.getHeader("X-User-Dept");

        if (userId == null) {
            return null;
        }

        return new User(userId, userName, userDept);
    }

    /**
     * 从方法参数中提取资源对象
     */
    private Document getResource(JoinPoint joinPoint, CheckPermission checkPermission) {
        Object[] args = joinPoint.getArgs();

        // 如果指定了参数名，则按名称匹配
        if (!checkPermission.resourceParam().isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(checkPermission.resourceParam())) {
                    return convertToDocument(args[i]);
                }
            }
        }

        // 查找ID参数并尝试通过ID获取资源
        String resourceId = getResourceIdFromParams(args);
        if (resourceId != null) {
            return documentService.getDocument(resourceId);
        }

        // 否则查找第一个 Document 类型参数
        for (Object arg : args) {
            if (arg instanceof Document) {
                return (Document) arg;
            }
        }

        return null;
    }

    /**
     * 从参数中提取资源ID
     */
    private String getResourceIdFromParams(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                // 简单判断：假设String类型的参数是资源ID
                return (String) arg;
            }
        }
        return null;
    }

    /**
     * 尝试将参数转换为 Document
     */
    private Document convertToDocument(Object obj) {
        if (obj instanceof Document) {
            return (Document) obj;
        }
        // 支持从 ID 查询 Document
        if (obj instanceof String) {
            return documentService.getDocument((String) obj);
        }
        return null;
    }


}
