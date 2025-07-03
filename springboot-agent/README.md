## 测试流程
1. maven package 打包agent
2. 设置app启动参数 `-javaagent:agent/target/agent-0.0.1-SNAPSHOT.jar`
3. 启动app, 访问 `http://localhost:8080/user/query` 
4. 查看监控数据