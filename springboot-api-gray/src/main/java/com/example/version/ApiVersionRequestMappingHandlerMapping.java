package com.example.version;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    
    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return (annotation != null) ? 
            new ApiVersionRequestCondition(annotation.value(), (HandlerMethod) null) : null;
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        if (annotation != null) {
            // 需要获取实际的HandlerMethod
            return new ApiVersionRequestCondition(annotation.value(), 
                new HandlerMethod(new Object(), method)); // 需要实际handler实例
        }
        return null;
    }
}