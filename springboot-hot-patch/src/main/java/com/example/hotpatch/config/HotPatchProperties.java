package com.example.hotpatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 热补丁配置属性
 */
@ConfigurationProperties(prefix = "hotpatch")
@Component
@Data
public class HotPatchProperties {
    /**
     * 是否启用热补丁功能
     */
    private boolean enabled = false;
    
    /**
     * 补丁文件存放路径
     */
    private String path = "./patches";
    
    /**
     * 允许的补丁文件最大大小（字节）
     */
    private long maxFileSize = 10 * 1024 * 1024;
    
    /**
     * 是否启用补丁签名验证
     */
    private boolean signatureVerification = false;
    
    /**
     * 允许执行热补丁操作的角色列表
     */
    private List<String> allowedRoles = List.of("ADMIN", "DEVELOPER");
    
    /**
     * 集群配置
     */
    private Cluster cluster = new Cluster();
    
    /**
     * 签名配置
     */
    private Signature signature = new Signature();
    
    @Data
    public static class Cluster {
        private boolean enabled = false;
        private String channel = "hotpatch:sync";
    }
    
    @Data
    public static class Signature {
        private String publicKey = "";
    }
}