package com.apidoc.model;

import java.util.List;

/**
 * API分组信息模型
 */
public class ApiGroup {
    private String name;
    private String description;
    private String version;
    private int order;
    private List<String> tags;
    private List<ApiInfo> apis;

    public ApiGroup() {}

    public ApiGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<ApiInfo> getApis() { return apis; }
    public void setApis(List<ApiInfo> apis) { this.apis = apis; }
}