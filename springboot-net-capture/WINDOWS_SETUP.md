# Windows 环境安装指南

## 🚨 重要提醒
在 Windows 系统上运行网络抓包功能，需要安装 **Npcap** 库。

## 📥 安装步骤

### 1. 下载 Npcap
- 访问官网：https://npcap.com/
- 点击 "Download" 下载最新版本
- 选择 "Npcap x.xx installer for Windows" 

### 2. 安装 Npcap
1. **以管理员身份运行**安装程序
2. 在安装选项中，**必须勾选**：
   - ✅ "Install Npcap in WinPcap API-compatible mode"
   - ✅ "Support raw 802.11 traffic"（可选）
3. 点击 "Install" 完成安装
4. 重启计算机（推荐）

### 3. 启动应用程序
1. **以管理员身份运行**命令行或IDE
2. 启动 Spring Boot 应用：
   ```bash
   mvn spring-boot:run
   ```
3. 访问 http://localhost:8080 验证功能

## ⚠️ 常见问题

### 问题1：Native library not found
**原因**：未安装 Npcap 或安装不完整
**解决**：重新安装 Npcap，确保勾选 WinPcap 兼容模式

### 问题2：No network interfaces found  
**原因**：权限不足
**解决**：以管理员权限运行应用程序

### 问题3：Access denied
**原因**：Windows 防火墙阻止
**解决**：
1. 临时关闭防火墙测试
2. 或添加 Java 程序到防火墙例外

## 🔧 验证安装

启动应用后，查看控制台日志：
- ✅ "WinPcap/Npcap 本地库加载成功"  
- ✅ "找到 X 个网络接口"

如果看到错误信息，请按上述步骤重新安装 Npcap。
