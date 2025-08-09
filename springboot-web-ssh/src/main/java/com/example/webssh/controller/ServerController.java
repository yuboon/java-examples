package com.example.webssh.controller;

import com.example.webssh.entity.ServerConfig;
import com.example.webssh.service.ServerService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/servers")
public class ServerController {
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 获取服务器列表
     */
    @GetMapping
    public ResponseEntity<List<ServerConfig>> getServers() {
        List<ServerConfig> servers = serverService.getAllServers();
        return ResponseEntity.ok(servers);
    }
    
    /**
     * 获取单个服务器配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServerConfig> getServer(@PathVariable Long id) {
        try {
            Optional<ServerConfig> server = serverService.getServerById(id);
            if (server.isPresent()) {
                return ResponseEntity.ok(server.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 添加服务器
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addServer(@RequestBody ServerConfig server) {
        try {
            // 验证必要参数
            if (server.getHost() == null || server.getHost().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "服务器地址不能为空"));
            }
            if (server.getUsername() == null || server.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
            }
            if (server.getPassword() == null || server.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码不能为空"));
            }
            
            // 设置默认值
            if (server.getPort() == null) {
                server.setPort(22);
            }
            if (server.getName() == null || server.getName().trim().isEmpty()) {
                server.setName(server.getUsername() + "@" + server.getHost());
            }
            
            Long serverId = serverService.saveServer(server);
            return ResponseEntity.ok(Map.of("success", true, "id", serverId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * 删除服务器
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteServer(@PathVariable Long id) {
        try {
            serverService.deleteServer(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * 测试服务器连接
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody ServerConfig server) {
        try {
            // 验证必要参数
            if (server.getHost() == null || server.getHost().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "服务器地址不能为空"));
            }
            if (server.getUsername() == null || server.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
            }
            if (server.getPassword() == null || server.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码不能为空"));
            }
            
            // 设置默认端口
            int port = server.getPort() != null ? server.getPort() : 22;
            
            // 简单的连接测试
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getUsername(), server.getHost(), port);
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000); // 5秒超时
            session.disconnect();
            
            return ResponseEntity.ok(Map.of("success", true, "message", "连接测试成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "连接测试失败: " + e.getMessage()));
        }
    }
}