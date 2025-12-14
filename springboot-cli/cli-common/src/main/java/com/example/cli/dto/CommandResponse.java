package com.example.cli.dto;

/**
 * CLI命令响应DTO
 */
public class CommandResponse {
    private boolean success;
    private String message;
    private String data;

    public CommandResponse() {}

    public CommandResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CommandResponse(boolean success, String message, String data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static CommandResponse success(String data) {
        return new CommandResponse(true, "Success", data);
    }

    public static CommandResponse error(String message) {
        return new CommandResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}