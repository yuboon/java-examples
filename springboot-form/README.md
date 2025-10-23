# 动态表单系统 - SpringBoot + JSON Schema

## 🎯 项目简介

这是一个基于 SpringBoot + JSON Schema 构建的动态表单验证系统，实现了"配置驱动开发"的理念。通过 JSON Schema 定义表单结构，系统自动生成前端表单界面和后端验证逻辑，大幅提升开发效率。

## ✨ 核心特性

- 🔧 **配置驱动**：只需定义 JSON Schema，自动生成完整表单系统
- ✅ **统一验证**：前后端共享同一套验证逻辑，确保数据一致性

## 🏗️ 技术架构

### 后端技术栈
- **SpringBoot 3.2.1** - 核心框架
- **JSON Schema Validator** - Schema验证引擎
- **Jackson** - JSON数据处理


## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+

### 启动步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd springboot-form
```

2. **编译运行**
```bash
mvn clean spring-boot:run
```

3. **访问应用**
```
浏览器打开: http://localhost:8080
```

## 📝 API接口

### 表单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/forms/schemas` | 获取所有可用表单 |
| GET | `/api/forms/{schemaId}/config` | 获取表单配置 |
| POST | `/api/forms/{schemaId}/submit` | 提交表单数据 |
| POST | `/api/forms/{schemaId}/validate-field` | 实时字段验证 |
| GET | `/api/forms/{schemaId}/submissions` | 获取表单提交数据 |
| GET | `/api/forms/statistics` | 获取系统统计信息 |

### 请求示例

#### 获取表单配置
```bash
curl -X GET "http://localhost:8080/api/forms/user-registration/config"
```

#### 提交表单数据
```bash
curl -X POST "http://localhost:8080/api/forms/user-registration/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123456",
    "confirmPassword": "Test123456"
  }'
```

## 🎨 预置表单

系统预置了两个示例表单：

### 1. 用户注册表单 (`user-registration`)
- 基础信息：用户名、邮箱、密码
- 个人信息：姓名、手机号、生日
- 兴趣偏好：多选兴趣标签
- 订阅设置：新闻通讯订阅

### 2. 满意度调查表单 (`satisfaction-survey`)
- 总体满意度评分
- 服务体验评价
- 推荐意愿
- 详细反馈

## 🔧 JSON Schema 示例

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "title": "用户注册表单",
  "required": ["username", "email", "password"],
  "properties": {
    "username": {
      "type": "string",
      "title": "用户名",
      "minLength": 3,
      "maxLength": 20,
      "pattern": "^[a-zA-Z0-9_]+$"
    },
    "email": {
      "type": "string",
      "title": "邮箱地址",
      "format": "email"
    },
    "password": {
      "type": "string",
      "title": "密码",
      "minLength": 8,
      "pattern": "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    }
  }
}
```