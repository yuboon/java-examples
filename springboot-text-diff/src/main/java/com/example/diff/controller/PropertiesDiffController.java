package com.example.diff.controller;

import com.example.diff.model.PropertiesDiffResult;
import com.example.diff.service.PropertiesDiffService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diff/properties")
@CrossOrigin(origins = "*")
public class PropertiesDiffController {

    private final PropertiesDiffService diffService;

    public PropertiesDiffController(PropertiesDiffService diffService) {
        this.diffService = diffService;
    }

    @PostMapping("/compare")
    public ResponseEntity<PropertiesDiffResult> compareProperties(@RequestBody DiffRequest request) {
        PropertiesDiffResult result = diffService.compareProperties(
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
