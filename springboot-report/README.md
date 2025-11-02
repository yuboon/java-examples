# 轻量级BI报表自助分析平台

一个基于Spring Boot 3 + 纯HTML/JS的轻量级报表系统，支持拖拽字段选择、动态SQL生成和多种图表展示。

## 功能特性

- 🔗 **数据源连接**: 支持MySQL数据库连接
- 📊 **元数据查询**: 自动获取数据库、表、字段信息
- 🎯 **拖拽操作**: 直观的拖拽界面选择维度和指标
- 📈 **多种图表**: 支持表格、柱状图、折线图、饼图
- 🔄 **动态SQL**: 根据用户选择自动生成SQL查询

## 技术栈

### 后端
- Spring Boot 3.2.0
- MySQL Connector
- Maven

### 前端
- HTML5 + JavaScript (ES6+)
- Tailwind CSS
- ECharts 5
- SortableJS (拖拽功能)

## 快速开始

### 1. 数据库准备

确保MySQL服务运行，执行测试数据脚本：

```bash
mysql -u root -p < setup_test_data.sql
```

或者手动执行SQL文件中的内容创建测试数据。

### 2. 配置数据库连接

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/information_schema?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 3. 启动应用

```bash
mvn clean install
mvn spring-boot:run
```

### 4. 访问应用

启动后，访问：http://localhost:8080/index.html
