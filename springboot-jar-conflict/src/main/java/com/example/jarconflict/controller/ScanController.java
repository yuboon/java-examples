package com.example.jarconflict.controller;

import com.example.jarconflict.advisor.ConflictAdvisor;
import com.example.jarconflict.detector.ConflictDetector;
import com.example.jarconflict.model.ConflictInfo;
import com.example.jarconflict.model.JarInfo;
import com.example.jarconflict.model.ScanResult;
import com.example.jarconflict.scanner.JarScanner;
import com.example.jarconflict.scanner.TestConflictDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/")
public class ScanController {
    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);

    @Autowired
    private JarScanner jarScanner;

    @Autowired
    private ConflictDetector conflictDetector;

    @Autowired
    private ConflictAdvisor conflictAdvisor;

    @Autowired
    private TestConflictDataGenerator testDataGenerator;

    @GetMapping("/api/test-scan")
    @ResponseBody
    public ResponseEntity<ScanResult> testScan() {
        logger.info("Starting test jar conflict scan with simulated data...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用模拟的冲突数据进行测试
            List<JarInfo> jars = testDataGenerator.generateTestConflictData();
            List<ConflictInfo> conflicts = conflictDetector.detectConflicts(jars);
            conflictAdvisor.generateAdvice(conflicts);
            
            long scanTime = System.currentTimeMillis() - startTime;
            
            ScanResult result = new ScanResult();
            result.setJars(jars);
            result.setConflicts(conflicts);
            result.setScanTimeMs(scanTime);
            result.setScanMode("test");
            result.setSummary(buildSummary(jars, conflicts));
            
            logger.info("Test scan completed in {}ms, found {} conflicts", scanTime, conflicts.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Test scan failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/api/scan")
    @ResponseBody
    public ResponseEntity<ScanResult> scan() {
        logger.info("Starting jar conflict scan...");
        long startTime = System.currentTimeMillis();
        
        try {
            List<JarInfo> jars = jarScanner.scanJars();
            List<ConflictInfo> conflicts = conflictDetector.detectConflicts(jars);
            conflictAdvisor.generateAdvice(conflicts);
            
            long scanTime = System.currentTimeMillis() - startTime;
            
            ScanResult result = new ScanResult();
            result.setJars(jars);
            result.setConflicts(conflicts);
            result.setScanTimeMs(scanTime);
            result.setScanMode("auto");
            result.setSummary(buildSummary(jars, conflicts));
            
            logger.info("Scan completed in {}ms, found {} conflicts", scanTime, conflicts.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Scan failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/jars")
    @ResponseBody
    public ResponseEntity<List<JarInfo>> getJars() {
        try {
            List<JarInfo> jars = jarScanner.scanJars();
            return ResponseEntity.ok(jars);
        } catch (Exception e) {
            logger.error("Failed to get jars", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/conflicts")
    @ResponseBody
    public ResponseEntity<List<ConflictInfo>> getConflicts() {
        try {
            List<JarInfo> jars = jarScanner.scanJars();
            List<ConflictInfo> conflicts = conflictDetector.detectConflicts(jars);
            conflictAdvisor.generateAdvice(conflicts);
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            logger.error("Failed to get conflicts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }

    private ScanResult.ScanSummary buildSummary(List<JarInfo> jars, List<ConflictInfo> conflicts) {
        ScanResult.ScanSummary summary = new ScanResult.ScanSummary();
        summary.setTotalJars(jars.size());
        summary.setTotalClasses(jars.stream()
            .mapToInt(jar -> jar.getClasses() != null ? jar.getClasses().size() : 0)
            .sum());
        summary.setConflictCount(conflicts.size());
        
        int critical = 0, high = 0, medium = 0, low = 0;
        for (ConflictInfo conflict : conflicts) {
            switch (conflict.getSeverity()) {
                case CRITICAL -> critical++;
                case HIGH -> high++;
                case MEDIUM -> medium++;
                case LOW -> low++;
            }
        }
        
        summary.setCriticalConflicts(critical);
        summary.setHighConflicts(high);
        summary.setMediumConflicts(medium);
        summary.setLowConflicts(low);
        
        return summary;
    }
}