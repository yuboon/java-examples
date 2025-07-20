package com.example.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// RpcClient.java
@Component
public class RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${rpc.server.url:http://localhost:8080}")
    private String serverUrl;
    
    /**
     * 发送RPC请求
     */
    public RpcResponse sendRequest(RpcRequest request, long timeout) {
        try {
            logger.debug("发送RPC请求: {}.{}", request.getClassName(), request.getMethodName());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RpcRequest> entity = new HttpEntity<>(request, headers);
            
            // 发送HTTP POST请求
            ResponseEntity<RpcResponse> responseEntity = restTemplate.postForEntity(
                serverUrl + "/rpc/invoke", 
                entity, 
                RpcResponse.class
            );
            
            RpcResponse response = responseEntity.getBody();
            logger.debug("收到RPC响应: requestId={}, success={}", 
                response.getRequestId(), response.isSuccess());
            
            return response;
            
        } catch (Exception e) {
            logger.error("RPC请求发送失败", e);
            
            RpcResponse errorResponse = new RpcResponse(request.getRequestId());
            errorResponse.setError("网络请求失败: " + e.getMessage());
            return errorResponse;
        }
    }
}