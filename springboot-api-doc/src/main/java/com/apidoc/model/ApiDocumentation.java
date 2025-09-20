package com.apidoc.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API文档完整信息模型
 */
public class ApiDocumentation {
    private String title;
    private String description;
    private String version;
    private String baseUrl;
    private LocalDateTime generateTime;
    private List<ApiGroup> groups;
    private List<ApiInfo> allApis;
    private List<String> environments;

    public ApiDocumentation() {
        this.generateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public LocalDateTime getGenerateTime() { return generateTime; }
    public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }

    public List<ApiGroup> getGroups() { return groups; }
    public void setGroups(List<ApiGroup> groups) { this.groups = groups; }

    public List<ApiInfo> getAllApis() { return allApis; }
    public void setAllApis(List<ApiInfo> allApis) { this.allApis = allApis; }

    public List<String> getEnvironments() { return environments; }
    public void setEnvironments(List<String> environments) { this.environments = environments; }
}