# Spring Boot 网络流量抓包分析系统

基于 Spring Boot + Pcap4j 构建的企业级网络流量监控和分析系统。

## 🚀 项目特性

- **多协议支持**: 支持 HTTP、TCP、UDP、DNS、DHCP 等多种网络协议解析
- **实时监控**: WebSocket 实时数据推送，支持在线流量监控
- **数据分析**: 提供流量统计、协议分析、IP排名等多维度数据分析
- **智能过滤**: 支持 BPF 过滤规则，可按协议、IP、端口等条件筛选
- **前后端分离**: 纯 HTML + 原生 JavaScript 实现，无需复杂的模板引擎
- **数据持久化**: 使用 H2 数据库存储抓包数据和统计信息
- **RESTful API**: 完整的 REST API 接口支持

## 📋 系统要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.6 或更高版本
- **操作系统**: Windows/Linux/macOS (需要管理员权限进行网络抓包)
- **网络权限**: 需要管理员权限访问网络接口

### Windows 额外要求

- 安装 **WinPcap** 或 **Npcap**
- 下载地址: https://npcap.com/

### Linux 额外要求

```bash
# Ubuntu/Debian
sudo apt-get install libpcap-dev

# CentOS/RHEL
sudo yum install libpcap-devel
```

### macOS 额外要求

```bash
# 使用 Homebrew
brew install libpcap
```

## 🛠️ 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd springboot-net-capture
```

### 2. 编译项目

```bash
mvn clean compile
```

### 3. 启动应用

```bash
# 使用管理员权限启动
sudo mvn spring-boot:run

# 或者编译后运行 JAR
mvn clean package
sudo java -jar target/springboot-net-capture-1.0.0.jar
```

### 4. 访问系统

- **主界面**: http://localhost:8080
- **数据包列表**: http://localhost:8080/packets.html
- **统计分析**: http://localhost:8080/statistics.html
- **实时监控**: http://localhost:8080/realtime.html
- **数据库控制台**: http://localhost:8080/h2-console

数据库连接信息:
- **URL**: `jdbc:h2:mem:netcapture`
- **用户名**: `sa`
- **密码**: `password`

## 🎯 使用指南

### 1. 开始抓包

1. 访问主页面 http://localhost:8080
2. 选择网络接口（可选，默认自动选择）
3. 设置过滤规则（可选，BPF 格式）
4. 点击"开始抓包"按钮

### 2. 查看数据包

- 访问数据包页面查看抓包数据
- 支持按协议、IP、端口等条件过滤
- 点击数据包行查看详细信息

### 3. 统计分析

- 访问统计分析页面查看流量统计
- 包含协议分布图、流量趋势图
- Top IP 地址排名
- 历史统计数据

### 4. 实时监控

- 访问实时监控页面进行实时监控
- WebSocket 实时数据流
- 可设置实时过滤条件
- 实时统计信息展示

## 📡 API 文档

### 抓包控制 API

#### 获取网络接口列表
```http
GET /api/capture/interfaces
```

#### 获取抓包状态
```http
GET /api/capture/status
```

#### 控制抓包
```http
POST /api/capture/control
Content-Type: application/json

{
  "action": "start",
  "networkInterface": "eth0",
  "filter": "tcp port 80"
}
```

### 数据包查询 API

#### 查询数据包
```http
GET /api/packets?protocol=HTTP&sourceIp=192.168.1.1&pageNo=1&pageSize=20
```

#### 获取数据包详情
```http
GET /api/packets/{id}
```

### 统计分析 API

#### 获取当前统计
```http
GET /api/statistics/current
```

#### 获取协议统计
```http
GET /api/statistics/protocols
```

#### 获取流量趋势
```http
GET /api/statistics/trend?hours=24
```

## ⚙️ 配置说明

主要配置位于 `src/main/resources/application.yml`:

```yaml
network:
  capture:
    # 默认网络接口（为空时自动选择）
    interface: ""
    # 抓包过滤器（BPF格式）
    filter: ""
    # 抓包缓冲区大小（字节）
    buffer-size: 65536
    # 抓包超时时间（毫秒）
    timeout: 1000
    # 是否启用混杂模式
    promiscuous: false
    # 最大抓包数量（0表示无限制）
    max-packets: 0
    # 是否自动启动抓包
    auto-start: false
    # 数据保留时间（小时）
    data-retention-hours: 24
```

## 🛠️ 开发说明

### 项目结构

```
src/main/java/com/example/netcapture/
├── config/          # 配置类
├── controller/      # Web控制器  
├── dto/            # 数据传输对象
├── entity/         # 数据库实体
├── repository/     # 数据访问层
├── service/        # 业务逻辑层
└── utils/          # 工具类

src/main/resources/
├── static/         # 静态资源
│   ├── css/        # 样式文件
│   ├── js/         # JavaScript文件
│   └── *.html      # HTML页面
└── application.yml # 应用配置
```

### 核心技术栈

- **Spring Boot 3.2**: 主框架
- **Pcap4j 1.8.2**: 网络抓包库
- **MyBatis Plus 3.5.5**: 数据访问框架
- **H2 Database**: 内存数据库
- **WebSocket**: 实时通信
- **HTML5 + 原生 JavaScript**: 前端实现
- **Bootstrap 5**: 前端UI框架

### 架构特点

- **前后端分离**: 纯静态 HTML + RESTful API
- **模块化设计**: 功能模块独立，易于扩展
- **异步处理**: WebSocket 实时通信，异步数据处理
- **响应式设计**: 支持多种设备屏幕尺寸

## 🔒 安全注意事项

1. **管理员权限**: 网络抓包需要管理员权限，请确保在安全的环境中运行
2. **数据安全**: 抓包数据可能包含敏感信息，请妥善保管
3. **网络影响**: 大量抓包可能影响网络性能，建议合理设置过滤规则
4. **法律合规**: 请确保网络监控符合当地法律法规要求

## 🐛 故障排除

### 1. 无法启动抓包

**错误**: Permission denied 或类似权限错误
**解决**: 使用管理员权限启动应用

### 2. 找不到网络接口

**错误**: No network interfaces found
**解决**: 
- 确保安装了 WinPcap/Npcap (Windows) 或 libpcap (Linux/macOS)
- 检查网络接口是否正常工作

### 3. WebSocket 连接失败

**错误**: WebSocket connection failed
**解决**:
- 检查防火墙设置
- 确认端口 8080 未被占用
- 浏览器是否支持 WebSocket
