package com.example.cmd.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class MyCommands {
    
    @ShellMethod("显示欢迎消息")
    public String hello(String name) {
        return "你好, " + name + "!";
    }
    
    @ShellMethod("计算两个数字的和")
    public int add(int a, int b) {
        return a + b;
    }

    // 注意: 如果方法名为驼峰式命名，则shell使用时需要使用-分隔符，如addBig -> add-big
    @ShellMethod(value = "计算两个数字的和,注意大小写")
    public int addBig(int a, int b) {
        return a + b;
    }

    // 关于驼峰命名也可以使用key属性进行指定，则shell使用时仍然是驼峰式命名，如addSmall -> addSmall
    @ShellMethod(value = "计算两个数字的和,注意大小写",key = {"addSmall"})
    public int addSmall(int a, int b) {
        return a + b;
    }
}