# ABAC 权限管理系统

基于 **JCasbin** 的属性访问控制（ABAC）演示项目，实现业务逻辑与权限逻辑的完全解耦。

## 🚀 快速开始

### 1. 启动后端

```bash
cd springboot-permission
mvn spring-boot:run
```

### 2. 访问前端

打开浏览器访问：http://localhost:8080

## 🎯 功能特性

### ✅ 核心功能

- **ABAC 权限模型**：基于用户和资源属性的动态授权
- **AOP 切面拦截**：通过 `@CheckPermission` 注解实现无侵入式权限控制
- **策略动态配置**：支持运行时添加/删除策略规则
- **前后端分离**：HTML + Tailwind CSS + Axios

### 📋 权限规则示例

| 规则 | 主体条件 | 资源条件 | 操作 |
|------|---------|---------|------|
| 同部门可编辑 | `r.sub.dept == r.obj.dept && r.sub.id == r.obj.ownerId` | `true` | edit |
| 同部门可读 | `r.sub.dept == r.obj.dept` | `true` | read |
| 所有者可删除 | `r.sub.id == r.obj.ownerId` | `true` | delete |
| 公开文档可读 | `true` | `r.obj.type == "public"` | read |

## 🧪 测试场景

### 场景 1：部门隔离
- 张三（研发部）只能编辑研发部的文档
- 李四（销售部）无法访问研发部文档

### 场景 2：所有权控制
- 文档创建者可以删除自己的文档
- 其他用户无法删除

### 场景 3：公开资源
- 所有用户都可以阅读 `type=public` 的文档

## 🔧 技术栈

- **后端**：Spring Boot 3.2.0 + JCasbin 1.55.0
- **前端**：HTML + Tailwind CSS + Axios
- **权限引擎**：JCasbin（支持 RBAC/ABAC/RBAC with domains）

## 📝 使用说明

### 1. 在业务代码中使用权限注解

```java
@CheckPermission(action = "edit")
@PutMapping("/{id}")
public Result<Document> update(@PathVariable String id, @RequestBody Document doc) {
    return Result.success(documentService.update(doc));
}
```

### 2. 策略配置

**模型文件（model.conf）**：
```
[request_definition]
r = sub, obj, act

[policy_definition]
p = sub_rule, obj_rule, act

[matchers]
m = eval(p.sub_rule) && eval(p.obj_rule) && r.act == p.act
```

**策略文件（policy.csv）**：
```
p, r.sub.dept == r.obj.dept, r.sub.id == r.obj.ownerId, edit
p, r.sub.dept == r.obj.dept, true, read
```