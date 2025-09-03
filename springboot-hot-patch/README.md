# Spring Boot Hot Patch Loader

一个基于Spring Boot 3的热补丁加载器，支持运行时动态替换Java类、Spring Bean和静态方法，无需重启应用即可修复线上紧急bug。

## ✨ 特性

- 🔥 **热补丁加载**：支持运行时动态加载补丁，无需重启应用
- 🎯 **多种替换类型**：支持Spring Bean、普通Java类、静态方法替换
- 💻 **管理界面**：提供友好的Web管理界面

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- Spring Boot 3.2+

### 构建和运行

1. **克隆项目**
```bash
git clone <repository-url>
cd springboot-hot-patch
```

2. **构建项目**
```bash
mvn clean package
```

3. **启动应用**（带Java Agent）
```bash
java -javaagent:agent/springboot-hot-patch-1.0.0-agent.jar -jar springboot-hot-patch-1.0.0.jar
```

4. **访问管理界面**
- 首页：http://localhost:8080
- 热补丁管理：http://localhost:8080/hotpatch
- 用户名/密码：admin/admin123

## 📖 使用指南

### 1. 创建补丁类

#### Spring Bean 补丁示例
```java
@HotPatch(
    type = PatchType.SPRING_BEAN,
    originalBean = "userService",
    version = "1.0.1",
    description = "修复getUserInfo空指针异常"
)
@Service
public class UserServicePatch {
    public String getUserInfo(Long userId) {
        if (userId == null) {
            return "未知用户"; // 修复空指针问题
        }
        // ... 其他逻辑
    }
}
```

#### 静态方法补丁示例
```java
@HotPatch(
    type = PatchType.STATIC_METHOD,
    originalClass = "com.example.utils.MathHelper",
    methodName = "divide",
    version = "1.0.3",
    description = "修复除零异常"
)
public class MathHelperDividePatch {
    public static int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return a / b;
    }
}
```

### 2. 打包补丁
```bash
# 编译补丁类
javac -cp "target/classes:lib/*" patches/UserServicePatch.java

# 打包为jar
jar cf UserService-1.0.1.jar -C target/classes patches/UserServicePatch.class

# 放到补丁目录
cp UserService-1.0.1.jar ./patches/
```

### 3. 加载补丁

#### 通过Web界面
1. 打开 http://localhost:8080/index.html
2. 选择补丁包
3. 点击"加载补丁"按钮

#### 通过API
```bash
curl -X POST "http://localhost:8080/api/hotpatch/load" \
     -d "patchName=UserService&version=1.0.1"
```

### 4. 测试补丁效果

访问测试接口验证补丁是否生效：
```bash
# 测试用户服务
curl "http://localhost:8080/api/test/user"

```

## 🔧 配置说明

### application.properties
```properties
# 热补丁配置
hotpatch.enabled=true
hotpatch.path=./patches


### JVM 启动参数
```bash
-javaagent:target/springboot-hot-patch-agent.jar
-XX:+UnlockDiagnosticVMOptions
-XX:+DebugNonSafepoints
```
