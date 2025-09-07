# Spring Boot 在线调试工具

基于 Java Agent + ByteBuddy 的运行时调试注入工具，支持在不重启应用的情况下动态注入调试日志。


## 📦 快速开始

### 1. 构建项目

```bash
cd springboot-online-debug
mvn clean package
```

### 2. 启动应用

```bash
# 方式1：使用 javaagent 启动
java -javaagent:target/springboot-online-debug-1.0.0.jar -jar target/springboot-online-debug-1.0.0.jar

# 方式2：启动后动态 attach（需要配置 JVM 参数）
java -jar target/springboot-online-debug-1.0.0.jar
```

### 3. 访问管理界面

打开浏览器访问：http://localhost:8080/index.html

## 🎯 使用示例

### 精确方法调试

```bash
# 通过 REST API 添加调试规则
curl -X POST http://localhost:8080/api/debug/method \
  -H "Content-Type: application/json" \
  -d '{"target": "com.example.onlinedebug.demo.UserService.getUserById"}'
```

### 类级别调试

```bash
# 调试整个类的所有方法
curl -X POST http://localhost:8080/api/debug/class \
  -H "Content-Type: application/json" \
  -d '{"target": "com.example.onlinedebug.demo.UserService"}'
```

### 包级别调试

```bash
# 调试整个包下的所有类
curl -X POST http://localhost:8080/api/debug/package \
  -H "Content-Type: application/json" \
  -d '{"target": "com.example.onlinedebug.demo"}'
```

### 正则模式调试

```bash
# 使用正则表达式匹配方法
curl -X POST http://localhost:8080/api/debug/pattern \
  -H "Content-Type: application/json" \
  -d '{"target": ".*Service.*\\.get.*"}'
```

## 📊 调试输出示例

当调试规则激活后，控制台会输出详细的调试信息：

```
[DEBUG-INJECT] com.example.onlinedebug.demo.UserService.getUserById() called with args: Long@123
[DEBUG-INJECT] com.example.onlinedebug.demo.UserService.getUserById() completed in 45ms returning: User@{id=123, name=...}
```


## ⚠️ 注意事项

1. **性能影响**：
   - 只有激活的调试规则才会产生性能开销
   - 避免在生产环境开启全局调试
   - 建议在排查完问题后及时清理调试规则

2. **安全考虑**：
   - 调试工具会暴露内部数据，请在安全环境中使用
   - 生产环境使用时建议启用认证机制
   - 避免调试包含敏感信息的方法