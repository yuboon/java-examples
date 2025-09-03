package com.example.hotpatch.controller;

import com.example.hotpatch.config.HotPatchProperties;
import com.example.hotpatch.core.HotPatchLoader;
import com.example.hotpatch.model.PatchInfo;
import com.example.hotpatch.model.PatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 热补丁管理控制器
 */
@RestController
@RequestMapping("/api/hotpatch")
@Slf4j
public class HotPatchController {
    
    private final HotPatchLoader patchLoader;
    private final HotPatchProperties properties;
    
    public HotPatchController(HotPatchLoader patchLoader, HotPatchProperties properties) {
        this.patchLoader = patchLoader;
        this.properties = properties;
    }
    
    @PostMapping("/load")
    public ResponseEntity<PatchResult> loadPatch(
            @RequestParam String patchName,
            @RequestParam String version) {
        
        log.info("请求加载热补丁: {}:{}", patchName, version);
        PatchResult result = patchLoader.loadPatch(patchName, version);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<PatchInfo>> listPatches() {
        List<PatchInfo> patches = patchLoader.getLoadedPatches();
        return ResponseEntity.ok(patches);
    }
    
    @PostMapping("/rollback")
    public ResponseEntity<PatchResult> rollbackPatch(
            @RequestParam String patchName) {
        
        log.info("请求回滚补丁: {}", patchName);
        PatchResult result = patchLoader.rollbackPatch(patchName);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Hot Patch Loader is running");
    }
    
    /**
     * 扫描可用的补丁文件
     */
    @GetMapping("/available")
    public ResponseEntity<List<Map<String, String>>> getAvailablePatches() {
        List<Map<String, String>> availablePatches = new ArrayList<>();
        
        try {
            // 获取补丁目录
            File patchDir = Paths.get(properties.getPath()).toFile();
            
            if (!patchDir.exists() || !patchDir.isDirectory()) {
                log.warn("补丁目录不存在: {}", properties.getPath());
                return ResponseEntity.ok(availablePatches);
            }
            
            // 扫描.jar文件
            File[] jarFiles = patchDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".jar"));
            
            if (jarFiles != null) {
                // 补丁文件名模式: PatchName-Version.jar
                Pattern pattern = Pattern.compile("^(.+?)-([\\d\\.]+)\\.jar$", Pattern.CASE_INSENSITIVE);
                
                for (File jarFile : jarFiles) {
                    String fileName = jarFile.getName();
                    Matcher matcher = pattern.matcher(fileName);
                    
                    if (matcher.matches()) {
                        String patchName = matcher.group(1);
                        String version = matcher.group(2);
                        
                        Map<String, String> patch = new HashMap<>();
                        patch.put("name", patchName);
                        patch.put("version", version);
                        patch.put("fileName", fileName);
                        patch.put("size", String.valueOf(jarFile.length()));
                        patch.put("lastModified", String.valueOf(jarFile.lastModified()));
                        
                        availablePatches.add(patch);
                        log.debug("发现补丁文件: {} v{}", patchName, version);
                    } else {
                        log.debug("跳过不符合命名规范的文件: {}", fileName);
                    }
                }
            }
            
            log.info("扫描补丁目录完成，发现 {} 个可用补丁", availablePatches.size());
            
        } catch (Exception e) {
            log.error("扫描补丁文件失败", e);
            return ResponseEntity.status(500).body(availablePatches);
        }
        
        return ResponseEntity.ok(availablePatches);
    }
}