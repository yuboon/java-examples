package com.example.rpc.core;

import cn.hutool.core.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Set;

@RestController
@RequestMapping("/rpc")
public class RpcRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
    
    @Autowired
    private ServiceRegistry serviceRegistry;
    
    /**
     * 处理RPC调用请求
     */
    @PostMapping("/invoke")
    public RpcResponse handleRpcRequest(@RequestBody RpcRequest request) {
        RpcResponse response = new RpcResponse(request.getRequestId());
        
        try {
            logger.debug("处理RPC请求: {}.{}", request.getClassName(), request.getMethodName());
            
            // 查找服务实例
            Object serviceInstance = serviceRegistry.getService(request.getClassName(), request.getVersion());
            if (serviceInstance == null) {
                response.setError("服务未找到: " + request.getClassName());
                return response;
            }
            
            // 通过反射调用方法
            Class<?> serviceClass = serviceInstance.getClass();
            Method method = serviceClass.getMethod(request.getMethodName(), request.getParameterTypes());

            Object[] parameters = request.getParameters();
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] != null && ClassUtil.isBasicType(paramTypes[i])) {
                    // 处理基本类型转换（如客户端传的是包装类型，服务端是基本类型）
                    parameters[i] = convertType(paramTypes[i], parameters[i]);
                }
            }

            Object result = method.invoke(serviceInstance, parameters);
            response.setResult(result);
            logger.debug("RPC调用成功: {}.{}", request.getClassName(), request.getMethodName());
            
        } catch (Exception e) {
            logger.error("RPC调用处理异常", e);
            response.setError("方法调用异常: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 类型转换处理（支持基本类型和包装类互转）
     */
    private Object convertType(Class<?> targetType, Object value) {
        // 处理null值
        if (value == null) return null;

        // 类型匹配时直接返回
        if (targetType.isInstance(value)) {
            return value;
        }

        // 处理数字类型转换
        if (value instanceof Number) {
            Number number = (Number) value;
            if (targetType == int.class || targetType == Integer.class) return number.intValue();
            if (targetType == long.class || targetType == Long.class) return number.longValue();
            if (targetType == double.class || targetType == Double.class) return number.doubleValue();
            if (targetType == float.class || targetType == Float.class) return number.floatValue();
            if (targetType == byte.class || targetType == Byte.class) return number.byteValue();
            if (targetType == short.class || targetType == Short.class) return number.shortValue();
        }

        // 处理布尔类型转换
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) return value;
            return Boolean.parseBoolean(value.toString());
        }

        // 处理字符类型转换
        if (targetType == char.class || targetType == Character.class) {
            String str = value.toString();
            if (!str.isEmpty()) return str.charAt(0);
        }

        throw new IllegalArgumentException(String.format(
                "类型转换失败: %s -> %s",
                value.getClass().getSimpleName(),
                targetType.getSimpleName()
        ));
    }
    
    /**
     * 查询已注册的服务列表
     */
    @GetMapping("/services")
    public Set<String> getRegisteredServices() {
        return serviceRegistry.getAllServices();
    }
}