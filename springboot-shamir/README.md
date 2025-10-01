# Shamir Secret Sharing - 五门三限密钥共享系统

基于 Shamir Secret Sharing (SSS) 算法的门限密钥共享演示系统，实现了"五门三限"的经典案例：将密钥拆分为 5 份，任意 3 份即可恢复。

## 项目特点

- ✅ **纯 Java 实现** - 基于拉格朗日插值和有限域运算，无需第三方算法库
- ✅ **前后端分离** - Spring Boot RESTful API + HTML/JS/TailwindCSS
- ✅ **交互友好** - 现代化 UI 设计，支持一键复制、实时验证
- ✅ **演示性强** - 内存存储（Map），适合学习和演示使用

⚠️ **注意**：本项目使用 `ConcurrentHashMap` 存储会话信息，仅用于演示。生产环境应使用数据库或分布式存储（如 Redis、MySQL）。

## 技术栈

### 后端
- Spring Boot 3.2.0
- Java 17
- Maven
- Lombok

### 前端
- HTML5
- TailwindCSS 3.x（通过 CDN）
- Vanilla JavaScript

## 快速开始

### 1. 克隆项目
```bash
cd springboot-shamir
```

### 2. 构建并运行
```bash
# 使用 Maven 构建
mvn clean package

# 运行 Spring Boot 应用
mvn spring-boot:run

# 或者直接运行 JAR
java -jar target/springboot-shamir-1.0.0.jar
```

### 3. 访问前端
打开浏览器访问：http://localhost:8080

## API 接口

### 1. 拆分密钥
**POST** `/api/shamir/split`

请求体：
```json
{
  "secret": "my-super-secret-key",
  "totalShares": 5,
  "threshold": 3
}
```

响应：
```json
{
  "sessionId": "uuid-xxx",
  "shares": [
    "1:a1b2c3d4...",
    "2:e5f6g7h8...",
    "3:i9j0k1l2...",
    "4:m3n4o5p6...",
    "5:q7r8s9t0..."
  ],
  "message": "密钥已拆分为 5 份，任意 3 份可恢复原始密钥"
}
```

### 2. 恢复密钥
**POST** `/api/shamir/combine`

请求体：
```json
{
  "shares": [
    "1:a1b2c3d4...",
    "3:i9j0k1l2...",
    "5:q7r8s9t0..."
  ]
}
```

响应：
```json
{
  "secret": "my-super-secret-key",
  "message": "成功使用 3 个份额恢复密钥",
  "success": true
}
```

### 3. 健康检查
**GET** `/api/shamir/health`

响应：`Shamir Secret Sharing Service is running`

## 算法原理

### Shamir Secret Sharing (SSS)

基于**拉格朗日插值定理**：
- 构造 t-1 次多项式：f(x) = a₀ + a₁x + a₂x² + ... + a_{t-1}x^{t-1}
- 其中 a₀ = secret（密钥）
- 生成 n 个点：(x₁, f(x₁)), (x₂, f(x₂)), ..., (xₙ, f(xₙ))
- 恢复时用拉格朗日公式计算 f(0) = a₀

### 数学保证
- 任意 t 个点可唯一确定多项式
- 少于 t 个点则有无穷多个可能的多项式
- 因此少于 t 份无法推导出密钥

## 应用场景

1. **金融安全** - 银行大额转账多人审批
2. **区块链多签** - 数字货币钱包多重签名
3. **云密钥管理** - 云服务商与用户共同持有密钥碎片
4. **企业权限控制** - 高危操作需多人参与
5. **数据备份** - 分布式密钥存储，防止单点故障


