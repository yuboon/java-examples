package com.example.dependencyscanner.model;

/**
 * 依赖信息模型
 * 
 
 */
public class DependencyInfo {
    
    private String groupId;
    private String artifactId;
    private String version;
    private String jarPath;
    
    public DependencyInfo() {}
    
    public DependencyInfo(String groupId, String artifactId, String version, String jarPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarPath = jarPath;
    }
    
    /**
     * 检查当前依赖是否匹配指定的漏洞
     * 
     * @param vulnerability 漏洞信息
     * @return 是否匹配
     */
    public boolean matches(Vulnerability vulnerability) {
        return this.groupId.equals(vulnerability.getGroupId()) && 
               this.artifactId.equals(vulnerability.getArtifactId());
    }
    
    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    @Override
    public String toString() {
        return "DependencyInfo{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", jarPath='" + jarPath + '\'' +
                '}';
    }
}