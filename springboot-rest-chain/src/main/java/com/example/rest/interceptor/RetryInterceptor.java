package com.example.rest.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;

@Slf4j
public class RetryInterceptor implements ClientHttpRequestInterceptor {
    private final int maxRetries;  // 最大重试次数

    public RetryInterceptor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        int retryCount = 0;
        while (true) {
            try {
                // 执行请求（调用下一个拦截器或实际请求）
                return execution.execute(request, body);
            } catch (HttpStatusCodeException e) {
                // 只对5xx服务器错误重试（业务错误不重试）
                if (e.getStatusCode().is5xxServerError() && retryCount < maxRetries) {
                    retryCount++;
                    log.warn("服务器错误，开始第{}次重试，状态码:{}", retryCount, e.getStatusCode());
                    continue;
                }
                throw e;  // 非5xx错误或达到最大重试次数
            } catch (IOException e) {
                // 网络异常重试（如连接超时、DNS解析失败）
                if (retryCount < maxRetries) {
                    retryCount++;
                    log.warn("网络异常，开始第{}次重试", retryCount, e);
                    continue;
                }
                throw e;
            }
        }
    }
}