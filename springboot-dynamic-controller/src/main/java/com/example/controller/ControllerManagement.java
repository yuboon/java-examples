package com.example.controller;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/controller")
public class ControllerManagement {

    @Autowired
    private DynamicControllerRegistry dynamicControllerRegistry;

    @PostMapping("/register")
    public String registerController(@RequestParam String controllerBeanName, @RequestParam String methodName) {
        Object controller = SpringUtil.getBean(controllerBeanName);
        String url = dynamicControllerRegistry.registerController(controller, methodName);
        return "Controller registered at: " + url;
    }

    @DeleteMapping("/unregister")
    public String unregisterController(@RequestParam String controllerBeanName, @RequestParam String methodName) {
        Object controller = SpringUtil.getBean(controllerBeanName);
        String url = dynamicControllerRegistry.unregisterController(controller.getClass(),methodName);
        return "Controller unregistered from: " + url;
    }
}