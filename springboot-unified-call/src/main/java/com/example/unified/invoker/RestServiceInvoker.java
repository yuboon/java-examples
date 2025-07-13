package com.example.unified.invoker;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.example.unified.ApiResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestServiceInvoker implements ServiceInvoker {
    private final RestTemplate restTemplate;

    private Environment environment;

    public RestServiceInvoker(RestTemplate restTemplate,Environment environment) {
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    @Override
    public <T> ApiResponse<T> invoke(String serviceName, String method, Object param, TypeReference<ApiResponse<T>> resultType) {
        String serviceUrl = environment.getProperty("service.direct-url." + serviceName);
        String url = serviceUrl + "/" + method;
        HttpEntity request = new HttpEntity<>(param);
        String result = restTemplate.postForObject(url, request, String.class);
        return JSONUtil.toBean(result, resultType, true);
    }
}