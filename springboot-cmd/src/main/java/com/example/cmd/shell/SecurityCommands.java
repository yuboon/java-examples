package com.example.cmd.shell;

import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

@ShellComponent
public class SecurityCommands {
    
    private boolean loggedIn = false;
    
    @ShellMethod("登录系统")
    public String login(String username, String password) {
        // 实现登录逻辑
        this.loggedIn = true;
        return "用户 " + username + " 已登录";
    }
    
    @ShellMethod("查看敏感信息")
    public String query() {
        return "这是敏感信息，只有登录后才能查看";
    }
    
    @ShellMethodAvailability("query")
    public Availability viewSecretInfoAvailability() {
        return loggedIn
            ? Availability.available()
            : Availability.unavailable("您需要先登录才能查看敏感信息");
    }
}