# Spring Boot CLI 通用命令系统

基于 Spring Boot + Spring Shell 的通用CLI系统，实现了"通用命令 + 动态分发"的设计模式，支持通过一个命令动态调用服务端的多个服务。

## 快速开始

### 1. 启动服务端

```bash
cd cli-server
mvn spring-boot:run
```

服务端将在 http://localhost:8080 启动

### 2. 启动客户端

```bash
cd cli-client
mvn spring-boot:run
```

### 3. 使用CLI命令

客户端启动后，进入Spring Shell交互模式，可使用以下命令：

```shell
# 查看帮助
help-exec

# 列出所有可用服务
list-services

# 用户服务示例
exec userService --args list
exec userService --args get 1
exec userService --args count admin

# 角色服务示例
exec roleService --args list
exec roleService --args users admin
exec roleService --args check 1 admin

# 系统服务示例
exec systemService --args status
exec systemService --args memory
exec systemService --args time
```