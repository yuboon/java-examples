package com.example.repository;

import com.example.entity.ConfigHistory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ConfigHistoryRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public ConfigHistoryRepository(@Qualifier("configJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<ConfigHistory> historyRowMapper = new RowMapper<ConfigHistory>() {
        @Override
        public ConfigHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            ConfigHistory history = new ConfigHistory();
            history.setId(rs.getLong("id"));
            history.setConfigKey(rs.getString("config_key"));
            history.setConfigType(rs.getString("config_type"));
            history.setOldValue(rs.getString("old_value"));
            history.setNewValue(rs.getString("new_value"));
            history.setEnvironment(rs.getString("environment"));
            history.setOperatorId(rs.getString("operator_id"));
            history.setChangeReason(rs.getString("change_reason"));
            history.setChangeTime(rs.getTimestamp("change_time") != null ? 
                rs.getTimestamp("change_time").toLocalDateTime() : null);
            return history;
        }
    };
    
    public List<ConfigHistory> findByConfigKeyAndEnvironmentOrderByChangeTimeDesc(String configKey, String environment) {
        String sql = "SELECT * FROM config_history WHERE config_key = ? AND environment = ? ORDER BY change_time DESC";
        return jdbcTemplate.query(sql, historyRowMapper, configKey, environment);
    }
    
    public List<ConfigHistory> findByEnvironmentOrderByChangeTimeDesc(String environment) {
        String sql = "SELECT * FROM config_history WHERE environment = ? ORDER BY change_time DESC";
        return jdbcTemplate.query(sql, historyRowMapper, environment);
    }
    
    public List<ConfigHistory> findByConfigTypeAndEnvironmentOrderByChangeTimeDesc(String configType, String environment) {
        String sql = "SELECT * FROM config_history WHERE config_type = ? AND environment = ? ORDER BY change_time DESC";
        return jdbcTemplate.query(sql, historyRowMapper, configType, environment);
    }
    
    public List<ConfigHistory> findByChangeTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM config_history WHERE change_time BETWEEN ? AND ? ORDER BY change_time DESC";
        return jdbcTemplate.query(sql, historyRowMapper, startTime, endTime);
    }
    
    public List<ConfigHistory> findByOperatorIdOrderByChangeTimeDesc(String operatorId) {
        String sql = "SELECT * FROM config_history WHERE operator_id = ? ORDER BY change_time DESC";
        return jdbcTemplate.query(sql, historyRowMapper, operatorId);
    }
    
    public ConfigHistory save(ConfigHistory history) {
        String sql = "INSERT INTO config_history (config_key, config_type, old_value, new_value, environment, operator_id, change_reason, change_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, history.getConfigKey(), history.getConfigType(), 
            history.getOldValue(), history.getNewValue(), history.getEnvironment(), 
            history.getOperatorId(), history.getChangeReason(), 
            history.getChangeTime() != null ? history.getChangeTime() : LocalDateTime.now());
        return history;
    }
    
    public Optional<ConfigHistory> findById(Long id) {
        String sql = "SELECT * FROM config_history WHERE id = ?";
        List<ConfigHistory> results = jdbcTemplate.query(sql, historyRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}