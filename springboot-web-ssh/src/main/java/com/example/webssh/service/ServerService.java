package com.example.webssh.service;

import com.example.webssh.entity.ServerConfig;
import com.example.webssh.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerService {
    
    @Autowired
    private ServerRepository serverRepository;
    
    public Long saveServer(ServerConfig server) {
        // 密码加密存储（生产环境建议）
        // server.setPassword(encryptPassword(server.getPassword()));
        return serverRepository.saveServer(server);
    }
    
    public List<ServerConfig> getAllServers() {
        List<ServerConfig> servers = serverRepository.findAllServers();
        // 不返回密码信息到前端
        servers.forEach(server -> server.setPassword(null));
        return servers;
    }
    
    public Optional<ServerConfig> getServerById(Long id) {
        return serverRepository.findServerById(id);
    }
    
    public void deleteServer(Long id) {
        serverRepository.deleteServer(id);
    }
}