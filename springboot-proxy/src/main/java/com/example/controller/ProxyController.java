package com.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Enumeration;

@RestController
@RequestMapping("/proxy")
@Slf4j
public class ProxyController {

    @Autowired
    private RestTemplate restTemplate;
    
    // 目标服务器基础URL
    private static final String TARGET_SERVER = "http://127.0.0.1:8081/";
    
    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        try {
            // todo 请求检查， 需要可以自定义相关逻辑并进行方法调用
            /*boolean valid = validateRequest(request);
            if(!valid){
                throw new IllegalArgumentException("bad param");
            }*/

            // 构建目标URL
            String requestUri = request.getRequestURI();
            String proxyPath = requestUri.substring("/proxy".length());
            String queryString = request.getQueryString();
            
            StringBuilder targetUrl = new StringBuilder(TARGET_SERVER);
            targetUrl.append(proxyPath);
            if (queryString != null) {
                targetUrl.append("?").append(queryString);
            }
            
            // 复制请求头
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // 排除一些不需要转发的头部
                if (!headerName.equalsIgnoreCase("host")) {
                    headers.set(headerName, request.getHeader(headerName));
                }
            }
            
            // 创建请求实体
            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            
            // 确定HTTP方法
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
            
            // 发送请求并获取响应
            ResponseEntity<String> responseEntity = 
                restTemplate.exchange(targetUrl.toString(), httpMethod, httpEntity, String.class);
            
            return responseEntity;
            
        } catch (Exception e) {
            log.error("代理请求失败: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("代理请求失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/**")
    public ResponseEntity<byte[]> proxyUpload(HttpServletRequest request,
                                              MultipartHttpServletRequest multipartRequest) {
        try {
            // 获取文件部分
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            multipartRequest.getFileMap().forEach((name, file) -> {
                try {
                    ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };
                    parts.add(name, resource);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // 获取其他表单参数
            multipartRequest.getParameterMap().forEach((name, values) -> {
                for (String value : values) {
                    parts.add(name, value);
                }
            });

            // 构建URL
            String requestUri = request.getRequestURI();
            String proxyPath = requestUri.substring("/proxy/upload".length());
            String targetUrl = TARGET_SERVER + "/upload" + proxyPath;

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 发送请求
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    targetUrl, HttpMethod.POST, requestEntity, byte[].class);

            return response;
        } catch (Exception e) {
            log.error("文件上传代理请求失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 示例：请求验证
    private boolean validateRequest(HttpServletRequest request) {
        // 实现验证逻辑，例如检查认证信息、IP白名单等
        String authToken = request.getHeader("Authorization");
        //return authToken != null && authService.isValidToken(authToken);
        return authToken != null;
    }
}