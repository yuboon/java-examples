package com.example.controller;

import com.example.scheduler.CronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/scheduler")
public class SchedulerController {
    
    @Autowired
    private CronRepository cronRepository;
    
    @GetMapping("/cron/{taskName}")
    public Map<String, String> getCron(@PathVariable String taskName) {
        Map<String, String> result = new HashMap<>();
        result.put("taskName", taskName);
        result.put("cron", cronRepository.getCronByTaskName(taskName));
        return result;
    }
    
    @PutMapping("/cron/{taskName}")
    public Map<String, String> updateCron(
            @PathVariable String taskName, 
            @RequestParam("cron") String cron) {
        cronRepository.updateCron(taskName, cron);
        
        Map<String, String> result = new HashMap<>();
        result.put("taskName", taskName);
        result.put("cron", cron);
        result.put("message", "Cron expression updated successfully");
        return result;
    }
}