package com.demo.shamir.controller;

import com.demo.shamir.dto.*;
import com.demo.shamir.service.ShamirService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Shamir Secret Sharing REST API
 */
@RestController
@RequestMapping("/api/shamir")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许跨域（生产环境应配置具体域名）
public class ShamirController {

    private final ShamirService shamirService;

    /**
     * 拆分密钥
     * POST /api/shamir/split
     */
    @PostMapping("/split")
    public ResponseEntity<SplitResponse> split(@RequestBody SplitRequest request) {
        try {
            SplitResponse response = shamirService.split(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new SplitResponse(null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SplitResponse(null, null, "服务器错误：" + e.getMessage()));
        }
    }

    /**
     * 恢复密钥
     * POST /api/shamir/combine
     */
    @PostMapping("/combine")
    public ResponseEntity<CombineResponse> combine(@RequestBody CombineRequest request) {
        try {
            CombineResponse response = shamirService.combine(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new CombineResponse(null, "服务器错误：" + e.getMessage(), false));
        }
    }

    /**
     * 健康检查
     * GET /api/shamir/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Shamir Secret Sharing Service is running");
    }
}