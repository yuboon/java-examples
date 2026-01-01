package com.example.diff.controller;

import com.example.diff.model.DiffResult;
import com.example.diff.service.DiffService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diff")
@CrossOrigin(origins = "*")
public class DiffController {

    private final DiffService diffService;

    public DiffController(DiffService diffService) {
        this.diffService = diffService;
    }

    /**
     * 比对两个文本的差异（Git 风格）
     */
    @PostMapping("/text")
    public ResponseEntity<DiffResult> compareText(@RequestBody DiffRequest request) {
        DiffResult result = diffService.compareConfigs(
                request.getOriginal(),
                request.getRevised()
        );
        return ResponseEntity.ok(result);
    }

    @Data
    public static class DiffRequest {
        private String original;
        private String revised;
    }
}
