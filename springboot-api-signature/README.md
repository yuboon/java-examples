# Spring Boot API Signature

基于 Spring Boot 和 HMAC-SHA256 的 API 接口签名验证解决方案，提供安全可靠的接口验证机制。

## 🚀 项目特性

- **安全性高**：采用成熟的 HMAC-SHA256 算法，确保签名的不可伪造性
- **易于集成**：基于 Spring Boot 拦截器机制，对现有代码侵入性小
- **防重放攻击**：通过时间戳验证有效防止请求重放
- **灵活配置**：支持多客户端、多密钥管理
- **完整示例**：提供完整的使用示例和测试用例

## 🛠️ 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Spring Boot 2.7.14

### 1. 克隆项目

```bash
git clone <repository-url>
cd springboot-api-signature
```

### 2. 构建项目

```bash
mvn clean compile
```

### 3. 运行应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## 📖 使用说明

### 签名生成算法

客户端需要按照以下规则生成签名：

1. **准备参数**：将所有请求参数（不包括签名本身）收集到 Map 中
2. **参数排序**：按参数名的字典序排序
3. **参数拼接**：将排序后的参数用 `&` 连接：`key1=value1&key2=value2`
4. **构建待签字符串**：`时间戳 + 参数字符串`
5. **生成签名**：使用 HMAC-SHA256 算法和密钥对待签字符串进行加密
6. **Base64 编码**：对加密结果进行 Base64 编码

### 请求头设置

客户端需要在请求头中包含以下字段：

- `X-Api-Key`: API 密钥标识
- `X-Timestamp`: 当前时间戳（秒级）
- `X-Signature`: 生成的签名

### 示例请求

```java
// 1. 准备参数
Map<String, Object> params = new HashMap<>();
params.put("userId", "12345");
params.put("type", "profile");

// 2. 生成时间戳
String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

// 3. 生成签名
String signature = SignatureUtils.generateSignature(params, timestamp, "your-secret");

// 4. 设置请求头
Headers headers = new Headers();
headers.set("X-Api-Key", "client1");
headers.set("X-Timestamp", timestamp);
headers.set("X-Signature", signature);

// 5. 发送请求
// GET /api/protected/data?userId=12345&type=profile
```

## 配置参数说明

| 参数 | 说明 | 默认值 |
|-----|------|--------|
| `api.security.enabled` | 是否启用签名验证 | `true` |
| `api.security.time-tolerance` | 时间戳容忍度（秒） | `300` |
| `api.security.enable-request-log` | 是否启用请求日志 | `true` |
| `api.security.enable-response-log` | 是否启用响应日志 | `false` |

## 🧪 测试

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 运行客户端测试

```java
// 运行 ApiClient 的 main 方法
// 或使用 curl 命令测试
```

### 3. 使用 curl 测试

```bash
# 1. 生成签名
timestamp=$(date +%s)
params="userId=12345&type=profile"
signature=$(echo -n "${timestamp}${params}" | openssl dgst -sha256 -hmac "demo-secret-key-for-client1-2024" -binary | base64)

# 2. 发送请求
curl -X GET "http://localhost:8080/api/protected/data?userId=12345&type=profile" \
  -H "X-Api-Key: client1" \
  -H "X-Timestamp: ${timestamp}" \
  -H "X-Signature: ${signature}"
```
