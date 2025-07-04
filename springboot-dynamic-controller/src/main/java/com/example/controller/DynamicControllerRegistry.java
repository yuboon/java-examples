package com.example.controller;

import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DynamicControllerRegistry {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final Map<String, String> registeredControllers = new HashMap<>();

    public String registerController(Object controller,String methodName) {
        Class<?> controllerClass = controller.getClass();
        String key = key(controllerClass,methodName);
        String url = "";
        if (!registeredControllers.containsKey(key)) {
            Method method = ReflectUtil.getMethod(controllerClass,methodName);
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);

            url = getMethodUrl(requestMapping,methodMapping);
            // 注册控制器
            requestMappingHandlerMapping.registerMapping(
                    RequestMappingInfo.paths(url).methods(methodMapping.method()).build(),
                    controller,method
            );
            registeredControllers.put(key,url);
        }else{
            url = registeredControllers.get(key);
            log.warn("controller already registered:{}",url);
        }
        return url;
    }

    public String unregisterController(Class<?> controllerClass,Method method) {
        RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        String url = getMethodUrl(requestMapping,methodMapping);
        RequestMappingInfo mappingInfo = RequestMappingInfo.paths(url).methods(methodMapping.method()).build();
        requestMappingHandlerMapping.unregisterMapping(mappingInfo);
        registeredControllers.remove(key(controllerClass,method.getName()));
        log.info("unregister controller:{}", url);
        return url;
    }

    public String unregisterController(Class<?> controllerClass,String methodName) {
        Method method = ReflectUtil.getMethod(controllerClass,methodName);
        return unregisterController(controllerClass,method);
    }

    private String key(Class<?> controllerClass, String method){
        return controllerClass.getName() + "." + method;
    }

    private String getMethodUrl(RequestMapping requestMapping,RequestMapping methodMapping){
        String baseUrl = "";
        String url = "";
        if(requestMapping != null){
            baseUrl = requestMapping.value()[0];
        }
        if(methodMapping != null) {
            String[] values = methodMapping.value();
            if (values.length > 0) {
                url = baseUrl + values[0];
            }
        }
        return url;
    }

}