package com.example.mutualcert.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTPS双向认证客户端示例
 *
 * 该类演示了如何在Java客户端中配置SSL双向认证，
 * 用于调用启用了双向认证的Spring Boot服务。
 */
public class SecureHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(SecureHttpClient.class);

    private final String serverUrl;
    private final RestTemplate restTemplate;

    /**
     * 构造函数
     * @param serverUrl 服务器地址，如: https://localhost:8443
     * @param keyStorePath 密钥库路径 (classpath资源)
     * @param keyStorePassword 密钥库密码
     * @param trustStorePath 信任库路径 (classpath资源)
     * @param trustStorePassword 信任库密码
     */
    public SecureHttpClient(String serverUrl, String keyStorePath, String keyStorePassword,
                       String trustStorePath, String trustStorePassword) {
        this.serverUrl = serverUrl;
        this.restTemplate = createRestTemplate(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }

    /**
     * 创建配置了双向认证的RestTemplate
     */
    private RestTemplate createRestTemplate(String keyStorePath, String keyStorePassword,
                                      String trustStorePath, String trustStorePassword) {
        try {
            // 创建SSL上下文
            SSLContext sslContext = createSSLContext(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);

            // 创建自定义的RestTemplate
            RestTemplate template = new RestTemplate();

            // 使用SimpleClientHttpRequestFactory并配置SSL上下文
            // 这种方式不需要额外的Apache HttpClient依赖
            template.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override
                protected java.net.HttpURLConnection openConnection(URL uri, Proxy proxy) throws IOException {
                    java.net.HttpURLConnection connection = super.openConnection(uri, proxy);

                    // 如果是HTTPS连接，配置SSL上下文
                    if (connection instanceof javax.net.ssl.HttpsURLConnection) {
                        javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) connection;
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> {
                            // 在生产环境中应该严格验证主机名，这里为了演示放宽限制
                            logger.warn("主机名验证已禁用，生产环境请启用: {}", hostname);
                            return true;
                        });
                    }

                    return connection;
                }
            });

            return template;

        } catch (Exception e) {
            logger.error("创建RestTemplate失败", e);
            throw new RuntimeException("创建RestTemplate失败", e);
        }
    }

    /**
     * 创建SSL上下文
     */
    private SSLContext createSSLContext(String keyStorePath, String keyStorePassword,
                                   String trustStorePath, String trustStorePassword) throws Exception {

        // 创建并初始化KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);
        kmf.init(keyStore, keyStorePassword.toCharArray());

        // 创建并初始化TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword);
        tmf.init(trustStore);

        // 创建SSL上下文
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        logger.info("SSL上下文创建成功");
        return sslContext;
    }

    /**
     * 加载密钥库
     */
    private KeyStore loadKeyStore(String path, String password) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("找不到密钥库文件: " + path);
            }

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, password.toCharArray());

            logger.info("密钥库加载成功: " + path);
            return keyStore;
        }
    }

    /**
     * 调用公共接口 (无需客户端证书)
     */
    public Map<String, Object> getPublicInfo() {
        try {
            logger.info("调用公共接口: {}", serverUrl + "/api/public/info");

            ResponseEntity<Map> response = restTemplate.getForEntity(
                serverUrl + "/api/public/info", Map.class);

            logger.info("公共接口调用成功，状态码: {}", response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            logger.error("公共接口调用失败，状态码: {}, 响应: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of("error", "请求失败", "status", e.getStatusCode().value());

        } catch (ResourceAccessException e) {
            logger.error("连接服务器失败: {}", e.getMessage());
            return Map.of("error", "连接服务器失败", "message", e.getMessage());

        } catch (Exception e) {
            logger.error("公共接口调用异常", e);
            return Map.of("error", "系统异常", "message", e.getMessage());
        }
    }

    /**
     * 调用需要认证的安全接口
     */
    public Map<String, Object> getSecureData() {
        try {
            logger.info("调用安全接口: {}", serverUrl + "/api/secure/data");

            ResponseEntity<Map> response = restTemplate.getForEntity(
                serverUrl + "/api/secure/data", Map.class);

            logger.info("安全接口调用成功，状态码: {}", response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            logger.error("安全接口调用失败，状态码: {}, 响应: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of("error", "认证失败", "status", e.getStatusCode().value());

        } catch (Exception e) {
            logger.error("安全接口调用异常", e);
            return Map.of("error", "系统异常", "message", e.getMessage());
        }
    }

    /**
     * 获取客户端证书信息
     */
    public Map<String, Object> getCertificateInfo() {
        try {
            logger.info("调用证书信息接口: {}", serverUrl + "/api/certificate/info");

            ResponseEntity<Map> response = restTemplate.getForEntity(
                serverUrl + "/api/certificate/info", Map.class);

            logger.info("证书信息获取成功，状态码: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            logger.error("获取证书信息失败", e);
            return Map.of("error", "获取证书信息失败", "message", e.getMessage());
        }
    }

    /**
     * 获取用户配置文件
     */
    public Map<String, Object> getUserProfile() {
        try {
            logger.info("调用用户配置接口: {}", serverUrl + "/api/user/profile");

            ResponseEntity<Map> response = restTemplate.getForEntity(
                serverUrl + "/api/user/profile", Map.class);

            logger.info("用户配置获取成功，状态码: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            logger.error("获取用户配置失败", e);
            return Map.of("error", "获取用户配置失败", "message", e.getMessage());
        }
    }

    /**
     * 提交数据到安全接口
     */
    public Map<String, Object> submitData(Map<String, Object> data) {
        try {
            logger.info("调用数据提交接口: {}", serverUrl + "/api/secure/submit");

            // 添加时间戳
            Map<String, Object> request = new HashMap<>(data);
            request.put("timestamp", Instant.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                serverUrl + "/api/secure/submit", entity, Map.class);

            logger.info("数据提交成功，状态码: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            logger.error("数据提交失败", e);
            return Map.of("error", "数据提交失败", "message", e.getMessage());
        }
    }

    /**
     * 执行完整的测试流程
     */
    public void runCompleteTest() {
        logger.info("=== 开始HTTPS双向认证客户端测试 ===");

        // 1. 测试公共接口
        System.out.println("\n🔓 1. 测试公共接口 (无需客户端证书)");
        Map<String, Object> publicInfo = getPublicInfo();
        System.out.println("响应: " + publicInfo);

        // 2. 测试安全接口
        System.out.println("\n🔐 2. 测试安全接口 (需要客户端证书)");
        Map<String, Object> secureData = getSecureData();
        System.out.println("响应: " + secureData);

        // 3. 获取证书信息
        System.out.println("\n📋 3. 获取客户端证书信息");
        Map<String, Object> certInfo = getCertificateInfo();
        System.out.println("响应: " + certInfo);

        // 4. 获取用户配置
        System.out.println("\n👤 4. 获取用户配置文件");
        Map<String, Object> userProfile = getUserProfile();
        System.out.println("响应: " + userProfile);

        // 5. 提交数据
        System.out.println("\n📤 5. 提交数据");
        Map<String, Object> dataToSubmit = Map.of(
            "message", "Hello from SecureHttpClient",
            "clientType", "Java",
            "version", "1.0.0"
        );
        Map<String, Object> submitResult = submitData(dataToSubmit);
        System.out.println("响应: " + submitResult);

        System.out.println("\n=== 测试完成 ===");
    }

    /**
     * 主方法 - 演示客户端调用
     */
    public static void main(String[] args) {
        // 配置参数
        String serverUrl = "https://localhost:8443";
        String keyStorePath = "client.jks";
        String keyStorePassword = "changeit";
        String trustStorePath = "truststore.jks";
        String trustStorePassword = "changeit";

        try {
            // 创建安全HTTP客户端
            SecureHttpClient client = new SecureHttpClient(
                serverUrl, keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);

            // 运行完整测试
            client.runCompleteTest();

        } catch (Exception e) {
            logger.error("客户端启动失败", e);
            System.err.println("错误: " + e.getMessage());
            System.err.println("请确保:");
            System.err.println("1. Spring Boot服务器正在运行 (https://localhost:8443)");
            System.err.println("2. 客户端证书文件存在且可访问");
            System.err.println("3. 证书配置正确");
        }
    }
}