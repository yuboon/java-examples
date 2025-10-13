# JWT 动态密钥轮换演示系统

这是一个Spring Boot JWT 动态密钥轮换演示系统，实现了 **RSA 2048 + KID + 定时轮换** 的安全生产实践。

## 🎯 功能特性

### 核心功能
- ✅ **动态密钥轮换**: 定时自动生成新的 RSA 密钥对
- ✅ **多版本密钥共存**: 新 Token 用新密钥，旧 Token 仍可验证
- ✅ **用户无感知**: 密钥轮换不影响已登录用户
- ✅ **KID 标识**: JWT Header 包含密钥 ID，支持多版本验证
- ✅ **定时清理**: 自动清理过期的密钥

### 演示功能
- 🔐 **用户认证**: 登录/退出、Token 生成和验证
- 🛡️ **受保护资源**: 需要有效 Token 才能访问
- 🔑 **密钥管理**: 查看密钥状态、手动轮换、清理过期密钥
- 🔍 **Token 解析**: 解析 JWT Header、Payload 和验证状态
- 📊 **系统监控**: 实时显示密钥存储和系统状态

## 🏗️ 技术架构

### 后端技术栈
- **Spring Boot 3.2.0** - 应用框架
- **JJWT 0.12.3** - JWT 处理库
- **RSA 2048** - 非对称加密算法
- **Spring Scheduling** - 定时任务

### 前端技术栈
- **HTML5 + CSS3** - 页面结构
- **JavaScript ES6** - 交互逻辑
- **Tailwind CSS** - UI 样式框架
- **Font Awesome** - 图标库

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 运行步骤

1. **克隆项目**
```bash
cd springboot-jwt-rotation
```

2. **编译运行**
```bash
mvn clean spring-boot:run
```

3. **访问应用**
```
浏览器打开: http://localhost:8080
```

### 测试账户
| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | password | 管理员 |
| user | 123456 | 普通用户 |
| test | test | 测试用户 |

## 🔧 核心组件

### 1. DynamicKeyStore - 动态密钥存储
- 管理 RSA 密钥对的生成、存储和获取
- 支持多版本密钥共存
- 线程安全的 ConcurrentHashMap 存储
- 自动密钥清理机制

### 2. JwtTokenService - JWT 服务
- Token 生成、验证和解析
- 支持多版本密钥验证
- Token 刷新功能
- KID (Key ID) 管理

### 3. KeyRotationScheduler - 密钥轮换调度
- 定时检查和轮换密钥
- 过期密钥清理
- 监控和日志记录
- 支持手动触发

## 📋 API 接口

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/validate` - Token 验证
- `POST /api/auth/refresh` - Token 刷新
- `GET /api/auth/me` - 获取当前用户信息

### 管理功能
- `POST /api/auth/admin/rotate-keys` - 手动轮换密钥
- `POST /api/auth/admin/cleanup-keys` - 清理过期密钥

### 演示功能
- `GET /api/demo/key-stats` - 获取密钥统计
- `POST /api/demo/parse-token` - 解析 Token
- `POST /api/demo/generate-test-token` - 生成测试 Token
- `GET /api/demo/protected` - 受保护资源
- `GET /api/demo/system-info` - 系统信息

## ⚙️ 配置说明

### application.yml
```yaml
jwt:
  token-expiration: 24          # Token过期时间（小时）
  rotation-period-days: 7       # 密钥轮换周期（天）
  grace-period-days: 14         # 旧密钥保留时间（天）
  key-size: 2048                # RSA密钥长度
```

### 定时任务
- **每天凌晨2点**: 检查是否需要轮换密钥
- **每天凌晨3点**: 清理过期密钥
- **每小时**: 密钥状态监控

## 🔄 密钥轮换流程

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   第1天         │    │   第7天          │    │   第21天        │
├─────────────────┤    ├──────────────────┤    ├─────────────────┤
│ 生成 key-001    │    │ 生成 key-002     │    │ 清理 key-001    │
│ 新Token使用     │→   │ 新Token使用     │→   │ 保留 key-002    │
│ key-001签名     │    │ key-002签名     │    │ key-002继续使用  │
│ key-001验证     │    │ key-001仍可验证  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 🎨 前端界面

### 功能页面
1. **用户登录** - 登录认证和状态显示
2. **受保护资源** - 演示 Token 保护
3. **密钥信息** - 实时密钥存储状态
4. **Token 解析** - JWT 结构分析工具
5. **管理功能** - 密钥轮换和清理

## 🧪 测试演示

### 基础流程
1. 打开 http://localhost:8080
2. 使用测试账户登录
3. 查看生成的 JWT Token 和 KID
4. 访问受保护资源验证 Token
5. 查看密钥存储状态

### 密钥轮换演示
1. 在管理页面手动轮换密钥
2. 观察新 Token 使用新密钥
3. 旧 Token 仍可正常验证
4. 查看密钥统计信息变化
