package com.example.sign.client;

import com.example.sign.util.SignatureUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API客户端调用示例
 *
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String API_KEY = "client1";
    private static final String SECRET = "demo-secret-key-for-client1-2024";
    private static final String CHARSET = "UTF-8";

    /**
     * 发送GET请求
     *
     * @param path 请求路径
     * @param params 请求参数
     * @return 响应结果
     */
    public static String sendGet(String path, Map<String, Object> params) {
        try {
            // 构建URL参数
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + path);
            if (params != null && !params.isEmpty()) {
                urlBuilder.append("?");
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (urlBuilder.toString().contains("?")) {
                        urlBuilder.append("&");
                    } else {
                        urlBuilder.append("?");
                    }
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }

            // 生成时间戳和签名
            String timestamp = SignatureUtil.getCurrentTimestamp();
            String signature = SignatureUtil.generateSignature(params, timestamp, SECRET);

            // 创建HTTP请求
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(urlBuilder.toString());

            // 设置请求头
            httpGet.setHeader("Content-Type", "application/json;charset=" + CHARSET);
            httpGet.setHeader("X-Api-Key", API_KEY);
            httpGet.setHeader("X-Timestamp", timestamp);
            httpGet.setHeader("X-Signature", signature);

            System.out.println("=== GET Request ===");
            System.out.println("URL: " + urlBuilder.toString());
            System.out.println("X-Api-Key: " + API_KEY);
            System.out.println("X-Timestamp: " + timestamp);
            System.out.println("X-Signature: " + signature);

            // 执行请求
            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), CHARSET);

            System.out.println("Response Status: " + response.getStatusLine().getStatusCode());
            System.out.println("Response Body: " + responseBody);
            System.out.println("===================");

            return responseBody;

        } catch (IOException e) {
            e.printStackTrace();
            return "{\"error\":\"Request failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 发送POST请求
     *
     * @param path 请求路径
     * @param params URL参数
     * @param requestBody 请求体
     * @return 响应结果
     */
    public static String sendPost(String path, Map<String, Object> params, String requestBody) {
        try {
            // 构建URL
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + path);
            if (params != null && !params.isEmpty()) {
                urlBuilder.append("?");
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                urlBuilder.setLength(urlBuilder.length() - 1); // 移除最后一个&
            }

            // 生成时间戳和签名
            String timestamp = SignatureUtil.getCurrentTimestamp();
            String signature = SignatureUtil.generateSignature(params, timestamp, SECRET);

            // 创建HTTP请求
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(urlBuilder.toString());

            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json;charset=" + CHARSET);
            httpPost.setHeader("X-Api-Key", API_KEY);
            httpPost.setHeader("X-Timestamp", timestamp);
            httpPost.setHeader("X-Signature", signature);

            // 设置请求体
            if (requestBody != null) {
                httpPost.setEntity(new StringEntity(requestBody, CHARSET));
            }

            System.out.println("=== POST Request ===");
            System.out.println("URL: " + urlBuilder.toString());
            System.out.println("Request Body: " + requestBody);
            System.out.println("X-Api-Key: " + API_KEY);
            System.out.println("X-Timestamp: " + timestamp);
            System.out.println("X-Signature: " + signature);

            // 执行请求
            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), CHARSET);

            System.out.println("Response Status: " + response.getStatusLine().getStatusCode());
            System.out.println("Response Body: " + responseBody);
            System.out.println("====================");

            return responseBody;

        } catch (IOException e) {
            e.printStackTrace();
            return "{\"error\":\"Request failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成签名工具方法
     *
     * @param params 参数
     * @return 签名信息
     */
    public static Map<String, String> generateSignatureInfo(Map<String, Object> params) {
        String timestamp = SignatureUtil.getCurrentTimestamp();
        String signature = SignatureUtil.generateSignature(params, timestamp, SECRET);

        Map<String, String> signatureInfo = new HashMap<>();
        signatureInfo.put("X-Api-Key", API_KEY);
        signatureInfo.put("X-Timestamp", timestamp);
        signatureInfo.put("X-Signature", signature);

        return signatureInfo;
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== API Client Test ===");

        // 测试1：GET请求 - 获取保护数据
        System.out.println("\n1. Testing GET /api/protected/data");
        Map<String, Object> getParams = new HashMap<>();
        getParams.put("userId", "12345");
        getParams.put("type", "profile");
        sendGet("/api/protected/data", getParams);

        // 测试2：GET请求 - 获取用户信息
        System.out.println("\n2. Testing GET /api/protected/user/67890");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("includeDetails", "true");
        sendGet("/api/protected/user/67890", userParams);

        // 测试3：POST请求 - 创建数据
        System.out.println("\n3. Testing POST /api/protected/create");
        Map<String, Object> postParams = new HashMap<>();
        String requestBody = "{\"name\":\"Test Product\",\"price\":99.99,\"category\":\"Electronics\"}";
        sendPost("/api/protected/create", postParams, requestBody);

        // 测试4：公开接口 - 健康检查
        System.out.println("\n4. Testing GET /api/public/health (no signature required)");
        sendGet("/api/public/health", null);

        // 测试5：生成签名工具
        System.out.println("\n5. Generate signature for custom parameters");
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("action", "test");
        customParams.put("userId", "999");
        customParams.put("timestamp", "1640995200");
        Map<String, String> signatureInfo = generateSignatureInfo(customParams);
        System.out.println("Generated Headers: " + signatureInfo);

        System.out.println("\n=== Test Complete ===");
    }
}