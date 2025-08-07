package com.example.repository;

import com.example.entity.ApplicationConfig;
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
public class ApplicationConfigRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public ApplicationConfigRepository(@Qualifier("configJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<ApplicationConfig> configRowMapper = new RowMapper<ApplicationConfig>() {
        @Override
        public ApplicationConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            ApplicationConfig config = new ApplicationConfig();
            config.setId(rs.getLong("id"));
            config.setConfigKey(rs.getString("config_key"));
            config.setConfigValue(rs.getString("config_value"));
            config.setConfigType(rs.getString("config_type"));
            config.setEnvironment(rs.getString("environment"));
            config.setDescription(rs.getString("description"));
            config.setEncrypted(rs.getBoolean("encrypted"));
            config.setRequiredRestart(rs.getBoolean("required_restart"));
            config.setActive(rs.getBoolean("active"));
            config.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            config.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            config.setCreatedBy(rs.getString("created_by"));
            config.setUpdatedBy(rs.getString("updated_by"));
            return config;
        }
    };
    
    public List<ApplicationConfig> findByConfigTypeAndEnvironmentAndActiveTrue(String configType, String environment) {
        String sql = "SELECT * FROM application_config WHERE config_type = ? AND environment = ? AND active = true";
        return jdbcTemplate.query(sql, configRowMapper, configType, environment);
    }
    
    public Optional<ApplicationConfig> findByConfigKeyAndEnvironmentAndActiveTrue(String configKey, String environment) {
        String sql = "SELECT * FROM application_config WHERE config_key = ? AND environment = ? AND active = true";
        List<ApplicationConfig> results = jdbcTemplate.query(sql, configRowMapper, configKey, environment);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<ApplicationConfig> findByEnvironmentAndActiveTrue(String environment) {
        String sql = "SELECT * FROM application_config WHERE environment = ? AND active = true";
        return jdbcTemplate.query(sql, configRowMapper, environment);
    }
    
    public List<ApplicationConfig> findByConfigTypeAndActiveTrue(String configType) {
        String sql = "SELECT * FROM application_config WHERE config_type = ? AND active = true";
        return jdbcTemplate.query(sql, configRowMapper, configType);
    }
    
    public Optional<ApplicationConfig> findByConfigKeyAndEnvironmentAndConfigTypeAndActiveTrue(
            String configKey, String environment, String configType) {
        String sql = "SELECT * FROM application_config WHERE config_key = ? AND environment = ? AND config_type = ? AND active = true";
        List<ApplicationConfig> results = jdbcTemplate.query(sql, configRowMapper, configKey, environment, configType);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<ApplicationConfig> findAllActiveConfigsByEnvironment(String environment) {
        String sql = "SELECT * FROM application_config WHERE environment = ? AND active = true ORDER BY config_type, config_key";
        return jdbcTemplate.query(sql, configRowMapper, environment);
    }
    
    public List<String> findDistinctConfigTypes() {
        String sql = "SELECT DISTINCT config_type FROM application_config WHERE active = true";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    
    public List<String> findDistinctEnvironments() {
        String sql = "SELECT DISTINCT environment FROM application_config WHERE active = true";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    
    public ApplicationConfig save(ApplicationConfig config) {
        if (config.getId() == null) {
            // Insert
            String sql = "INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, required_restart, active, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql, config.getConfigKey(), config.getConfigValue(), config.getConfigType(), 
                config.getEnvironment(), config.getDescription(), config.getEncrypted(), 
                config.getRequiredRestart(), config.getActive(), now, now, 
                config.getCreatedBy(), config.getUpdatedBy());
        } else {
            // Update
            String sql = "UPDATE application_config SET config_value = ?, description = ?, encrypted = ?, required_restart = ?, active = ?, updated_at = ?, updated_by = ? WHERE id = ?";
            jdbcTemplate.update(sql, config.getConfigValue(), config.getDescription(), 
                config.getEncrypted(), config.getRequiredRestart(), config.getActive(), 
                LocalDateTime.now(), config.getUpdatedBy(), config.getId());
        }
        return config;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM application_config WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public Optional<ApplicationConfig> findById(Long id) {
        String sql = "SELECT * FROM application_config WHERE id = ?";
        List<ApplicationConfig> results = jdbcTemplate.query(sql, configRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}