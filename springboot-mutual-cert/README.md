# Spring Boot HTTPS双向认证演示项目

这是一个Spring Boot HTTPS双向认证演示项目，展示了如何在Spring Boot应用中实现客户端和服务器的双向SSL认证。

## 🚀 快速开始

### 1. 环境准备

确保你的环境中已安装：
- JDK 17+
- Maven 3.6+
- OpenSSL (用于生成证书)

### 2. 启动应用

```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

应用启动后将在以下端口提供服务：
- HTTP: http://localhost:8080 (自动重定向到HTTPS)
- HTTPS: https://localhost:8443 (需要客户端证书)

## 🧪 测试验证

### 1. 公共接口测试 (无需客户端证书)

```bash
# 使用curl测试公共接口
curl -k https://localhost:8443/api/public/info
```

### 2. 需要认证的接口测试 (需要客户端证书)

```bash
# 使用客户端证书测试安全接口
curl -k --cert certs/client.p12:changeit \
     https://localhost:8443/api/secure/data

# 获取证书信息
curl -k --cert certs/client.p12:changeit \
     https://localhost:8443/api/certificate/info

# 获取用户配置文件
curl -k --cert certs/client.p12:changeit \
     https://localhost:8443/api/user/profile
```

### 3. POST请求测试

```bash
# 提交数据 (需要客户端证书)
curl -k --cert certs/client.p12:changeit \
     -H "Content-Type: application/json" \
     -d '{"message": "Hello Server", "data": [1, 2, 3]}' \
     https://localhost:8443/api/secure/submit
```