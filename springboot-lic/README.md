# 许可证控制系统

基于RSA2048的Spring Boot许可证控制系统，支持Windows和Linux主板序列号绑定。

## 功能特性

- ✅ RSA2048公私钥签名验签
- ✅ 主板序列号硬件绑定（Windows/Linux）
- ✅ 许可证有效期控制
- ✅ 功能权限管理
- ✅ Web演示界面（前后端分离）
- ✅ REST API接口

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- Windows或Linux操作系统

### 2. 启动应用

```bash
# 编译项目
mvn clean package

# 启动应用
mvn spring-boot:run

# 或者直接运行jar包
java -jar target/springboot-lic-1.0.0.jar
```

### 3. 访问演示界面

打开浏览器访问：`http://localhost:8080`

## API接口

### 密钥管理

- `POST /api/keys/generate` - 生成新密钥对
- `POST /api/keys/load` - 加载密钥
- `GET /api/keys/status` - 检查密钥状态

### 许可证操作

- `POST /api/license/generate` - 生成许可证
- `POST /api/license/verify` - 验证许可证

### 硬件信息

- `GET /api/hardware/info` - 获取硬件信息

## 使用说明

### 1. 生成密钥对

首先在Web界面点击"生成新密钥对"按钮，系统会自动生成RSA2048密钥对。

### 2. 创建许可证

填写许可证信息：
- 软件名称
- 授权给（公司名称）
- 到期时间
- 功能权限（逗号分隔）

点击"生成许可证"即可创建签名的许可证文件。

### 3. 验证许可证

将许可证JSON内容粘贴到验证区域，点击"验证许可证"进行验证。

系统会检查：
- 签名有效性
- 硬件指纹匹配
- 有效期

## 项目结构

```
src/main/java/com/license/
├── controller/     # REST控制器
├── entity/         # 实体类
├── service/        # 业务服务
├── util/           # 工具类
└── config/         # 配置类

src/main/resources/
├── static/         # 前端静态文件
└── application.yml # 应用配置
```

## 技术栈

- **后端**: Spring Boot 3.x, Java Security API
- **前端**: HTML, JavaScript, TailwindCSS
- **加密**: RSA2048, SHA256withRSA
- **序列化**: Jackson JSON

## 注意事项

1. **密钥安全**: 私钥应妥善保管，不要泄露
2. **硬件绑定**: 更换主板后需要重新生成许可证
3. **生产环境**: 建议将密钥存储在安全的密钥管理系统中
4. **Linux权限**: Linux下获取硬件信息可能需要sudo权限

## 许可证格式

```json
{
  "subject": "MyApp",
  "issuedTo": "Company Name",
  "hardwareId": "MOTHERBOARD_SERIAL",
  "expireAt": "2025-12-31",
  "features": ["BASIC", "EXPORT", "REPORT"],
  "signature": "Base64_Signature"
}
```