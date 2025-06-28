package com.example.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseCronRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public String getCronByTaskName(String taskName) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT cron FROM scheduled_tasks WHERE task_name = ?",
                String.class,
                taskName
            );
        } catch (EmptyResultDataAccessException e) {
            return "0 0/1 * * * ?";  // 默认值
        }
    }
    
    public void updateCron(String taskName, String cron) {
        // 验证cron表达式
        try {
            new CronTrigger(cron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
        }
        
        // 更新数据库
        int updated = jdbcTemplate.update(
            "UPDATE scheduled_tasks SET cron = ? WHERE task_name = ?",
            cron, taskName
        );
        
        if (updated == 0) {
            // 如果记录不存在，则插入
            jdbcTemplate.update(
                "INSERT INTO scheduled_tasks (task_name, cron) VALUES (?, ?)",
                taskName, cron
            );
        }
    }
}