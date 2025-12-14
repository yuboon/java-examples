package com.example.cli.dto;

import java.util.List;

/**
 * CLI命令请求DTO
 */
public class CommandRequest {
    private String service;
    private List<String> args;

    public CommandRequest() {}

    public CommandRequest(String service, List<String> args) {
        this.service = service;
        this.args = args;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }
}