package com.example.permission.controller;

import com.example.permission.common.Result;
import com.example.permission.service.EnforcerService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 策略管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "*")
public class PolicyController {

    @Autowired
    private EnforcerService enforcerService;

    /**
     * 获取所有策略
     */
    @GetMapping
    public Result<List<List<String>>> list() {
        List<List<String>> policies = enforcerService.getAllPolicy();
        return Result.success(policies);
    }

    /**
     * 添加策略
     */
    @PostMapping
    public Result<Boolean> add(@RequestBody PolicyRequest request) {
        boolean success = enforcerService.addPolicy(
                request.getSubRule(),
                request.getObjRule(),
                request.getAction()
        );

        if (success) {
            enforcerService.savePolicy();
        }

        return Result.success(success);
    }

    /**
     * 删除策略
     */
    @DeleteMapping
    public Result<Boolean> remove(@RequestBody PolicyRequest request) {
        boolean success = enforcerService.removePolicy(
                request.getSubRule(),
                request.getObjRule(),
                request.getAction()
        );

        if (success) {
            enforcerService.savePolicy();
        }

        return Result.success(success);
    }

    @Data
    public static class PolicyRequest {
        private String subRule;
        private String objRule;
        private String action;
    }
}
