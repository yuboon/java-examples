package com.example.asn1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 *
 * 
 * @version 1.0.0
 */
@Controller
public class HomeController {

    /**
     * 首页
     *
     * @return 首页视图
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}