package com.example.unified;

import cn.hutool.core.lang.TypeReference;
import com.example.unified.config.ServiceConfig;
import com.example.unified.invoker.ServiceInvoker;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UnifiedServiceClient {
    private final Map<String, ServiceInvoker> invokerMap;
    private final ServiceConfig serviceConfig;

    public UnifiedServiceClient(List<ServiceInvoker> invokers, ServiceConfig serviceConfig) {
        this.invokerMap = invokers.stream()
                .collect(Collectors.toMap(invoker -> invoker.getClass().getSimpleName(), Function.identity()));
        this.serviceConfig = serviceConfig;
    }

    public <T> ApiResponse<T> call(String serviceName, String method, Object param, TypeReference<ApiResponse<T>> resultType) {
        // 根据配置选择调用方式
        String protocol = serviceConfig.getProtocol(serviceName);
        ServiceInvoker invoker = protocol.equals("rpc") ? 
            invokerMap.get("DubboServiceInvoker") : 
            invokerMap.get("RestServiceInvoker");
        return invoker.invoke(serviceName, method, param,resultType);
    }
}