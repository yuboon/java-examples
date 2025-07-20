package com.example.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class RpcClientProxy {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);
    
    @Autowired
    private RpcClient rpcClient;
    
    /**
     * 为指定接口创建代理实例
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, String version, long timeout) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class[]{interfaceClass},
            new RpcInvocationHandler(interfaceClass, version, timeout, rpcClient)
        );
    }
    
    /**
     * 动态代理调用处理器
     */
    private static class RpcInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final String version;
        private final long timeout;
        private final RpcClient rpcClient;
        
        public RpcInvocationHandler(Class<?> interfaceClass, String version, long timeout, RpcClient rpcClient) {
            this.interfaceClass = interfaceClass;
            this.version = version;
            this.timeout = timeout;
            this.rpcClient = rpcClient;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 跳过Object类的基础方法
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }
            
            // 构建RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            
            try {
                // 发送远程调用请求
                RpcResponse response = rpcClient.sendRequest(request, timeout);
                
                if (response.isSuccess()) {
                    return response.getResult();
                } else {
                    throw new RuntimeException("RPC调用失败: " + response.getError());
                }
            } catch (Exception e) {
                logger.error("RPC调用异常: {}.{}", interfaceClass.getName(), method.getName(), e);
                throw new RuntimeException("RPC调用异常", e);
            }
        }
        
        /**
         * 构建RPC请求对象
         */
        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            RpcRequest request = new RpcRequest();
            request.setClassName(interfaceClass.getName());
            request.setMethodName(method.getName());
            request.setParameterTypes(method.getParameterTypes());
            request.setParameters(args);
            request.setVersion(version);
            return request;
        }
    }
}