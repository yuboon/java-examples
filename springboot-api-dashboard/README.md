# Spring Boot API 监控面板

## 项目简介
基于 Spring Boot 3 实现的轻量级 API 性能监控面板，采用**分级采样**策略，支持时间段筛选和接口名搜索功能。

## 技术实现

### 分级采样策略
```
├── 秒级桶 (最近5分钟)   - 300个桶，精确到秒
├── 分钟级桶 (最近1小时)  - 60个桶，分钟聚合  
├── 小时级桶 (最近24小时) - 24个桶，小时聚合
└── 天级桶 (最近7天)     - 7个桶，天聚合
```

## 技术栈
- **后端**: Spring Boot 3.2.1 + Spring AOP + Lombok
- **前端**: 纯 HTML + TailwindCSS + Alpine.js
- **Java 版本**: 17+

## 快速开始

### 1. 编译运行
```bash
cd springboot-api-dashboard
mvn clean install
mvn spring-boot:run
```

### 2. 访问界面
- 监控面板: http://localhost:8080/index.html
- API 接口: http://localhost:8080/api/metrics

### 3. 测试接口
项目内置了演示接口用于测试：
- `GET /api/demo/hello` - 简单接口
- `GET /api/demo/users` - 用户列表
- `POST /api/demo/users` - 创建用户（模拟偶尔失败）
- `GET /api/demo/products?limit=10` - 商品列表
- `GET /api/demo/orders/{orderId}` - 订单查询
- `GET /api/demo/slow` - 慢接口（2-5秒）
- `GET /api/demo/error` - 错误接口（50%失败率）

## API 接口文档

### 获取监控指标（支持筛选）
```
GET /api/metrics?startTime=1640995200000&endTime=1641000000000&methodFilter=user
```
**参数：**
- `startTime` - 开始时间戳（毫秒）
- `endTime` - 结束时间戳（毫秒）
- `methodFilter` - 接口名过滤关键字

### 获取汇总统计（支持筛选）
```
GET /api/metrics/summary?startTime=1640995200000&endTime=1641000000000&methodFilter=user
```

### 快捷时间查询
```
GET /api/metrics/recent/5?methodFilter=demo    # 最近5分钟
GET /api/metrics/recent/60?methodFilter=user   # 最近1小时
```

### 清空所有指标
```
DELETE /api/metrics
```

### 清理过期指标
```
DELETE /api/metrics/stale?maxAgeMs=3600000
```

## 界面功能详解

### 时间范围筛选
- **快捷按钮**: 5分钟、30分钟、1小时、6小时、24小时
- **自定义时间**: 精确指定开始和结束时间
- **智能桶选择**: 根据时间跨度自动选择最适合的数据粒度

### 接口搜索
- **实时搜索**: 输入关键字实时过滤接口
- **模糊匹配**: 支持接口名模糊匹配
- **搜索防抖**: 500ms防抖，避免频繁请求

### 数据展示
- **汇总卡片**: 监控接口数、总调用数、错误数、平均响应时间
- **详细表格**: 每个接口的完整统计数据
- **多维排序**: 支持按任意列排序
- **成功率进度条**: 直观显示接口成功率

## 使用场景
- ✅ **性能分析** - 快速找到最慢的方法
- ✅ **稳定性监控** - 发现失败次数多的接口  
- ✅ **容量评估** - 统计高频调用方法，辅助扩容
- ✅ **临时排障** - 替代复杂APM，用于中小规模应用
- ✅ **趋势分析** - 通过时间筛选分析接口性能趋势
- ✅ **问题定位** - 通过搜索快速定位特定接口问题

## 注意事项
- 监控数据存储在内存中，重启后会丢失
- 适合单机部署，不支持集群数据聚合  
- 内存占用固定，适合高并发生产环境
- 数据精度在可接受范围内，适合APM监控场景