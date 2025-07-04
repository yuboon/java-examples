package com.example.config;

import cn.hutool.core.util.ClassUtil;
import com.example.controller.DynamicController;
import com.example.controller.DynamicControllerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

@Configuration
@Slf4j
public class WebConfig {

    @Autowired
    private DynamicControllerRegistry dynamicControllerRegistry;

    @Bean
    public Void unregisterDynamicController() {
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("com.example", DynamicController.class);
        for(Class<?> clazz : classes) {
            DynamicController dynamicController = clazz.getAnnotation(DynamicController.class);
            boolean needRegister = dynamicController.startupRegister();
            if(needRegister) {
                continue;
            }

            // 默认不需要注册的controller，需要在启动时注销掉
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            Method[] methods = clazz.getDeclaredMethods();
            for(Method method : methods) {
                dynamicControllerRegistry.unregisterController(clazz,method);
            }
        }

        return null;
    }
}