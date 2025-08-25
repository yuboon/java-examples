package com.example.dependencyscanner.model;

/**
 * 依赖风险模型 - 用于返回扫描结果
 * 
 
 */
public class DependencyRisk {
    
    private String groupId;
    private String artifactId;
    private String version;
    private String riskLevel;
    private String cve;
    private String description;
    private String safeVersion;
    private String vulnerableVersions;  // 新增：影响版本范围
    private String reference;
    
    public DependencyRisk() {}
    
    public DependencyRisk(DependencyInfo dependency, Vulnerability vulnerability) {
        this.groupId = dependency.getGroupId();
        this.artifactId = dependency.getArtifactId();
        this.version = dependency.getVersion();
        this.riskLevel = vulnerability.getSeverity();
        this.cve = vulnerability.getCve();
        this.description = vulnerability.getDescription();
        this.safeVersion = vulnerability.getSafeVersion();
        this.vulnerableVersions = vulnerability.getVulnerableVersions(); // 新增
        this.reference = vulnerability.getReference();
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
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public String getCve() {
        return cve;
    }
    
    public void setCve(String cve) {
        this.cve = cve;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSafeVersion() {
        return safeVersion;
    }
    
    public void setSafeVersion(String safeVersion) {
        this.safeVersion = safeVersion;
    }
    
    public String getVulnerableVersions() {
        return vulnerableVersions;
    }
    
    public void setVulnerableVersions(String vulnerableVersions) {
        this.vulnerableVersions = vulnerableVersions;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    @Override
    public String toString() {
        return "DependencyRisk{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", cve='" + cve + '\'' +
                ", description='" + description + '\'' +
                ", safeVersion='" + safeVersion + '\'' +
                ", vulnerableVersions='" + vulnerableVersions + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}