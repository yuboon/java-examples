package com.example.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class RpcServiceProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(RpcServiceProcessor.class);
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有@RpcService注解
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if (rpcService != null) {
            registerRpcService(bean, rpcService);
        }
        
        return bean;
    }
    
    /**
     * 注册RPC服务
     */
    private void registerRpcService(Object serviceBean, RpcService rpcService) {
        ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
        
        Class<?> interfaceClass = rpcService.value();
        if (interfaceClass == void.class) {
            // 如果没有指定接口，自动查找第一个接口
            Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                interfaceClass = interfaces[0];
            } else {
                logger.warn("无法确定服务接口: {}", serviceBean.getClass().getName());
                return;
            }
        }
        
        serviceRegistry.registerService(interfaceClass, rpcService.version(), serviceBean);
    }
}