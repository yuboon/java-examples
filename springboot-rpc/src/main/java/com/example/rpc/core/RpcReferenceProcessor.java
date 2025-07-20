package com.example.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class RpcReferenceProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(RpcReferenceProcessor.class);
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 处理所有标注了@RpcReference的字段
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                injectRpcReference(bean, field, rpcReference);
            }
        }
        
        return bean;
    }
    
    /**
     * 注入RPC服务代理
     */
    private void injectRpcReference(Object bean, Field field, RpcReference rpcReference) {
        try {
            RpcClientProxy rpcClientProxy = applicationContext.getBean(RpcClientProxy.class);
            
            Class<?> interfaceClass = field.getType();
            Object proxyInstance = rpcClientProxy.createProxy(
                interfaceClass, 
                rpcReference.version(), 
                rpcReference.timeout()
            );
            
            field.setAccessible(true);
            field.set(bean, proxyInstance);
            
            logger.info("注入RPC服务代理: {}", interfaceClass.getName());
            
        } catch (Exception e) {
            logger.error("RPC服务代理注入失败: {}", field.getName(), e);
            throw new RuntimeException("RPC服务代理注入失败", e);
        }
    }
}