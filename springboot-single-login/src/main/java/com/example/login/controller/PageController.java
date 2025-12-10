package com.example.login.controller;

import com.example.login.model.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 页面控制器
 */
@Controller
public class PageController {

    /**
     * API首页 - 检查登录状态
     */
    @GetMapping({"/api", "/api/status"})
    @ResponseBody
    public ApiResponse<?> apiStatus() {
        return ApiResponse.success("API服务正常");
    }
}