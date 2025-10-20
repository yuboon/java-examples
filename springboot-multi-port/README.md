# Spring Boot 双端口应用示例

这是一个实现双端口监听的 Spring Boot 应用，能够同时提供用户端和管理端服务。

## 功能特性

- ✅ 双端口监听（8082用户端 + 8083管理端）
- ✅ 基于注解的API路由分离
- ✅ 端口感知的拦截器和异常处理
- ✅ 分端口的日志记录
- ✅ 健康检查和监控

## 项目结构

```
springboot-multi-port/
├── src/
│   ├── main/
│   │   ├── java/com/example/multiport/
│   │   │   ├── annotation/           # API注解
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/          # 控制器
│   │   │   ├── exception/           # 异常处理
│   │   │   ├── interceptor/         # 拦截器
│   │   │   ├── logging/             # 日志记录
│   │   │   ├── model/               # 数据模型
│   │   │   ├── monitoring/          # 监控组件
│   │   │   ├── service/             # 服务层
│   │   │   └── MultiPortApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/                        # 测试类
├── docker-compose.yml
├── Dockerfile
├── nginx/
│   └── conf.d/
├── k8s/
│   └── deployment.yaml
└── pom.xml
```

## 快速开始

### 本地运行

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd springboot-multi-port
   ```

2. **构建项目**
   ```bash
   mvn clean package
   ```

3. **运行应用**
   ```bash
   java -jar target/springboot-multi-port-1.0.0.jar
   ```

4. **测试接口**

   **用户端接口 (端口8082):**
   ```bash
   # 获取商品列表
   curl http://localhost:8082/api/user/products

   # 搜索商品
   curl http://localhost:8082/api/user/products/search?keyword=iPhone

   # 健康检查
   curl http://localhost:8082/health/user
   ```

   **管理端接口 (端口8083):**
   ```bash
   # 获取所有商品（包括下架的）
   curl http://localhost:8083/api/admin/products

   # 获取统计信息
   curl http://localhost:8083/api/admin/statistics/products

   # 健康检查
   curl http://localhost:8083/health/admin
   ```

## API接口文档

### 用户端API (端口8082)

#### 商品相关

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/user/products` | 获取所有上架商品 |
| GET | `/api/user/products/{id}` | 获取商品详情 |
| GET | `/api/user/products/category/{category}` | 按分类获取商品 |
| GET | `/api/user/products/search` | 搜索商品 |

#### 购物车相关

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/user/cart/{userId}` | 获取用户购物车 |
| POST | `/api/user/cart/{userId}/items` | 添加商品到购物车 |
| PUT | `/api/user/cart/{userId}/items/{cartItemId}` | 更新购物车商品数量 |
| DELETE | `/api/user/cart/{userId}/items/{cartItemId}` | 从购物车移除商品 |
| DELETE | `/api/user/cart/{userId}` | 清空购物车 |
| GET | `/api/user/cart/{userId}/summary` | 获取购物车统计信息 |

### 管理端API (端口8083)

#### 商品管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/admin/products` | 获取所有商品 |
| GET | `/api/admin/products/{id}` | 获取商品详情 |
| POST | `/api/admin/products` | 创建商品 |
| PUT | `/api/admin/products/{id}` | 更新商品 |
| DELETE | `/api/admin/products/{id}` | 删除商品 |
| PATCH | `/api/admin/products/{id}/status` | 更新商品状态 |
| PATCH | `/api/admin/products/batch/status` | 批量更新商品状态 |

#### 统计分析

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/admin/statistics/products` | 商品统计信息 |
| GET | `/api/admin/statistics/overview` | 系统概览 |
| GET | `/api/admin/statistics/ports` | 端口状态 |

## 配置说明

### application.yml

```yaml
# 双端口配置
dual:
  port:
    user-port: 8082    # 用户端端口
    admin-port: 8083   # 管理端端口

# Spring配置
spring:
  application:
    name: multi-port-application

# 日志配置
logging:
  level:
    com.example.multiport: DEBUG
```

## 核心技术

- **双端口配置**: 通过`TomcatServletWebServerFactory`配置多个Connector
- **API路由分离**: 使用自定义注解`@UserApi`和`@AdminApi`配合`WebMvcConfigurer`
- **端口感知拦截器**: 根据请求端口执行不同的处理逻辑
- **分端口日志**: 为不同端口使用独立的Logger实例
- **健康检查**: 为每个端口提供独立的健康检查端点