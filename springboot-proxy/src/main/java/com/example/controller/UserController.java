package com.example.controller;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @RequestMapping("/save")
    public ResponseEntity<String> addUser(@RequestBody Map<String,String> data) {
        log.info("save user :{}", JSONUtil.toJsonStr(data));
        return ResponseEntity.ok("ok");
    }
}