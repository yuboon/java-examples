# Spring Boot 时间轮

这是一个基于Spring Boot的时间轮（Timing Wheel）实现项目，提供了完整的时间轮功能和可视化的监控界面。

## 🚀 项目特性

- ✅ **高效时间轮算法**：O(1)时间复杂度的任务调度
- ✅ **前后端分离架构**：独立的前端页面，纯静态部署
- ✅ **实时可视化监控**：动态时间轮图表、任务状态分布、性能统计
- ✅ **完整的RESTful API**：支持任务的CRUD操作

## 🚀 快速启动

### 1. 启动后端服务

```bash
cd springboot-timingwheel
mvn spring-boot:run -Dmaven.test.skip=true
```

后端服务将启动在：http://localhost:8080

### 2. 访问前端页面

直接打开 `http://localhost:8080/index.html` 文件即可访问监控页面。

## 📊 功能特性

### 时间轮核心功能
- **高效调度**: O(1)时间复杂度的任务添加和删除
- **批量处理**: 同一槽位的多个任务可以批量触发
- **多层支持**: 理论支持多层时间轮处理不同精度需求

### 监控界面功能
- **实时统计**: 总任务数、已完成、失败、活跃任务数
- **时间轮可视化**: 圆形时间轮实时动画展示
- **任务管理**: 创建、取消、查看任务详情
- **状态监控**: 任务状态分布饼图
- **配置管理**: API地址配置、刷新间隔调整

### API接口
- `GET /api/timingwheel/stats` - 获取时间轮统计信息
- `GET /api/timingwheel/execution-stats` - 获取执行统计
- `GET /api/timingwheel/tasks` - 获取活跃任务列表
- `POST /api/timingwheel/tasks/sample` - 创建示例任务
- `POST /api/timingwheel/tasks/batch` - 批量创建任务
- `DELETE /api/timingwheel/tasks/{taskId}` - 取消任务
- `POST /api/timingwheel/cleanup` - 清理已完成任务

## ⚙️ 配置说明

### 后端配置 (application.yml)
```yaml
timingwheel:
  config:
    slot-size: 512              # 槽位数量
    tick-duration: 100          # 时间间隔（毫秒）
    worker-threads: 4           # 工作线程数
    enable-multi-wheel: true    # 启用多层时间轮
    enable-metrics: true        # 启用监控指标
    task-timeout: 30000         # 任务超时时间（毫秒）
```

## 🎯 使用示例

### 创建单个任务
```javascript
// 前端调用
const result = await window.apiManager.createSampleTask('simple', 2000);

// 后端API调用
curl -X POST http://localhost:8080/api/timingwheel/tasks/sample \
  -H "Content-Type: application/json" \
  -d '{"type": "simple", "delay": 2000}'
```

### 批量创建任务
```javascript
// 前端调用
const result = await window.apiManager.createBatchTasks(10, 1000, 5000);

// 后端API调用
curl -X POST http://localhost:8080/api/timingwheel/tasks/batch \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "minDelay": 1000, "maxDelay": 5000}'
```