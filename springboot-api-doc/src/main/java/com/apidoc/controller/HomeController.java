package com.apidoc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 */
@Controller
public class HomeController {

    /**
     * 首页重定向到API文档界面
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }
}