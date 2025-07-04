package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@DynamicController(startupRegister = false)
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Dynamic Controller!";
    }

}