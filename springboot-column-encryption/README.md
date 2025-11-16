# Spring Boot 字段级加密演示项目

## 🎯 项目概述

这是一个基于 Spring Boot 3 + MyBatis 的字段级加解密演示项目，实现了**透明**的字段级加密功能。通过简单的 `@Encrypted` 注解，即可实现敏感数据的自动加密存储和解密读取。

### ✨ 核心特性

- 🔐 **透明加密**：业务代码零侵入，自动加解密
- 🛡️ **安全算法**：使用 AES-GCM 加密算法，支持防篡改
- 🚀 **零配置**：注解驱动，开箱即用
- 🔧 **可扩展**：支持自定义加密算法和密钥管理

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+

### 2. 运行项目

```bash
# 克隆项目
git clone <repository-url>
cd springboot-column-encryption

# 编译运行
mvn spring-boot:run
```

### 3. 访问应用

- **前端界面**：http://localhost:8080
- **API接口**：http://localhost:8080/api/users
- **H2控制台**：http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - 用户名: `sa`
  - 密码: `password`

## 📖 使用指南

### 基本用法

1. **在实体类字段上添加注解**：

```java
@Data
public class User {
    private Long id;
    private String username;

    @Encrypted  // 添加此注解即可实现自动加密
    private String phone;

    @Encrypted
    private String idCard;

    // 普通字段不会加密
    private Integer age;
}
```

2. **正常使用 MyBatis 操作**：

```java
// 插入数据 - 自动加密敏感字段
User user = new User();
user.setUsername("张三");
user.setPhone("13812345678");  // 会自动加密存储
user.setIdCard("110101199001011234");  // 会自动加密存储
userMapper.insert(user);

// 查询数据 - 自动解密敏感字段
User result = userMapper.findById(user.getId());
System.out.println(result.getPhone());  // 输出: 13812345678 (已自动解密)
```