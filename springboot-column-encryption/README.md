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

### 支持的加密字段

- ✅ 手机号
- ✅ 身份证号
- ✅ 邮箱
- ✅ 银行卡号
- ✅ 家庭住址
- ✅ 其他字符串类型敏感信息

## 🔧 核心原理

### 1. 注解机制

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    Algorithm algorithm() default Algorithm.AES_GCM;
    boolean searchable() default false;
}
```

### 2. TypeHandler 自动处理

```java
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String value, JdbcType jdbcType) {
        // 写入数据库时自动加密
        ps.setString(i, CryptoUtil.encrypt(value));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) {
        // 从数据库读取时自动解密
        return CryptoUtil.decrypt(rs.getString(columnName));
    }
}
```

### 3. 自动注册机制

通过 `ObjectWrapperFactory` 自动识别标记了 `@Encrypted` 注解的字段，并注册相应的 TypeHandler。

## 🔐 安全特性

### 加密算法
- **算法名称**：AES-GCM
- **密钥长度**：256位
- **IV长度**：12字节（随机生成）
- **认证标签**：128位

### 密钥管理
```java
// 默认密钥（生产环境请使用安全的密钥管理系统）
String secretKey = "MySecretKey12345MySecretKey12345";

// 支持运行时更新密钥
CryptoUtil.updateKey(newSecretKey);

// 生成随机密钥
String randomKey = CryptoUtil.generateRandomKey();
```

## 📊 API 接口

### 用户管理接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 获取用户详情 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/users/search` | 搜索用户 |
| GET | `/api/users/stats` | 获取统计信息 |

### 请求示例

```json
POST /api/users
{
    "username": "张三",
    "phone": "13812345678",
    "idCard": "110101199001011234",
    "email": "zhangsan@example.com",
    "bankCard": "6222021234567890123",
    "address": "北京市朝阳区建国路88号",
    "age": 34,
    "gender": "男",
    "occupation": "软件工程师"
}
```

```json
{
    "success": true,
    "message": "用户创建成功",
    "data": {
        "id": 1,
        "username": "张三",
        "phone": "13812345678",  // 已自动解密
        "idCard": "110101199001011234",  // 已自动解密
        "email": "zhangsan@example.com",
        "bankCard": "6222021234567890123",
        "address": "北京市朝阳区建国路88号",
        "age": 34,
        "gender": "男",
        "occupation": "软件工程师",
        "enabled": true,
        "createTime": "2024-01-01 10:00:00",
        "updateTime": "2024-01-01 10:00:00"
    }
}
```

## 🔍 查看加密效果

### 数据库中的存储格式

在 H2 控制台中查看 users 表，可以看到加密字段存储的是加密后的密文：

```sql
SELECT phone, id_card, email FROM users WHERE username = '张三';
```

输出示例：
```
phone    | "AbCdEfGhIjKlMnOp:sDeFgHiJkLmNoPqRsTuVwXyZ123456789"
id_card  | "XyZaBcDeFgHiJkLm:PqRsTuVwXyZ1234567890AbCdEfGhIj"
email    | "MnOpQrStUvWxYzA:bCdEfGhIjKlMnOpQrStUvWxYzA123456"
```

### 应用中的显示格式

通过 API 查询时，自动返回解密后的明文：

```json
{
    "phone": "13812345678",
    "idCard": "110101199001011234",
    "email": "zhangsan@example.com"
}
```