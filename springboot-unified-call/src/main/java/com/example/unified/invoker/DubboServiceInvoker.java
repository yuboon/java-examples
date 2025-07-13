package com.example.unified.invoker;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.unified.ApiResponse;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DubboServiceInvoker implements ServiceInvoker {
    private final SimpleReferenceCache referenceCache;
    private final Environment environment;

    public DubboServiceInvoker(SimpleReferenceCache referenceCache, Environment environment) {
        this.referenceCache = referenceCache;
        this.environment = environment;
    }

    @Override
    public <T> ApiResponse<T> invoke(String serviceName, String method, Object param,TypeReference<ApiResponse<T>> resultType) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        String interfaceName = environment.getProperty("dubbo.reference." + serviceName + ".interfaceName");
        reference.setInterface(interfaceName);
        reference.setGeneric("true");
        reference.setRegistry(new RegistryConfig("N/A"));
        reference.setVersion("1.0.0");
        // 从配置文件读取直连地址（优先级：代码 > 配置文件）
        String directUrl = environment.getProperty("dubbo.reference." + serviceName + ".url");
        if (StrUtil.isNotEmpty(directUrl)) {
            reference.setUrl(directUrl);  // 设置直连地址，覆盖注册中心发现
        }

        GenericService service = referenceCache.get(reference);
        Object[] params = {param};
        Object result = service.$invoke(method, getParamTypes(params), params);
        JSONObject jsonObject = new JSONObject(result);
        ApiResponse<T> response = JSONUtil.toBean(jsonObject, resultType,true);
        return response;
    }

    private String[] getParamTypes(Object[] params) {
        return Arrays.stream(params).map(p -> p.getClass().getName()).toArray(String[]::new);
    }
}