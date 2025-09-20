package com.apidoc.model;

import java.util.List;
import java.util.Map;

/**
 * API文档信息模型
 */
public class ApiInfo {
    private String method;
    private String path;
    private String summary;
    private String description;
    private String group;
    private String version;
    private String status;
    private String statusLabel;
    private String statusCss;
    private List<String> environments;
    private List<ParameterInfo> parameters;
    private ReturnTypeInfo returnType;
    private Map<String, Object> examples;
    private List<String> tags;

    public ApiInfo() {}

    public ApiInfo(String method, String path, String summary) {
        this.method = method;
        this.path = path;
        this.summary = summary;
    }

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getStatusCss() { return statusCss; }
    public void setStatusCss(String statusCss) { this.statusCss = statusCss; }

    public List<String> getEnvironments() { return environments; }
    public void setEnvironments(List<String> environments) { this.environments = environments; }

    public List<ParameterInfo> getParameters() { return parameters; }
    public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }

    public ReturnTypeInfo getReturnType() { return returnType; }
    public void setReturnType(ReturnTypeInfo returnType) { this.returnType = returnType; }

    public Map<String, Object> getExamples() { return examples; }
    public void setExamples(Map<String, Object> examples) { this.examples = examples; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}