package com.example.diff.controller;

import com.example.diff.model.DiffResult;
import com.example.diff.service.YamlDiffService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diff/yaml")
@CrossOrigin(origins = "*")
public class YamlDiffController {

    private final YamlDiffService yamlDiffService;

    public YamlDiffController(YamlDiffService yamlDiffService) {
        this.yamlDiffService = yamlDiffService;
    }

    @PostMapping("/compare")
    public ResponseEntity<DiffResult> compareYaml(@RequestBody DiffRequest request) {
        try {
            DiffResult result = yamlDiffService.compareYaml(
                    request.getOriginal(),
                    request.getRevised()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/html")
    public ResponseEntity<String> compareYamlHtml(@RequestBody DiffRequest request) {
        try {
            DiffResult result = yamlDiffService.compareYaml(
                    request.getOriginal(),
                    request.getRevised()
            );
            return ResponseEntity.ok(result.toHtml());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Data
    public static class DiffRequest {
        private String original;
        private String revised;
    }
}
