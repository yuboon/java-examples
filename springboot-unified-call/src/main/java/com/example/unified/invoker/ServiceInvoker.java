package com.example.unified.invoker;

import cn.hutool.core.lang.TypeReference;
import com.example.unified.ApiResponse;

public interface ServiceInvoker {
    <T> ApiResponse<T> invoke(String serviceName, String method, Object param, TypeReference<ApiResponse<T>> resultType);
}