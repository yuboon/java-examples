package com.example.cmd.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellCommandGroup;

@ShellComponent
@ShellCommandGroup("文件操作")
public class FileCommands {
    
    @ShellMethod("列出目录内容")
    public String ls(String path) {
        // 实现列出目录内容的逻辑
        return "列出 " + path + " 的内容";
    }
    
    @ShellMethod("创建新目录")
    public String mkdir(String dirName) {
        // 实现创建目录的逻辑
        return "创建目录: " + dirName;
    }
}