package com.example.cmd.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class AdvancedCommands {
    
    @ShellMethod("使用高级参数选项的示例")
    public String greet(
        @ShellOption(defaultValue = "世界") String name,
        @ShellOption(help = "决定是否使用大写", defaultValue = "false") boolean uppercase
    ) {
        String greeting = "你好, " + name + "!";
        return uppercase ? greeting.toUpperCase() : greeting;
    }
}