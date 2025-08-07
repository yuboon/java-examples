-- ==========================================
-- Spring Boot 动态配置数据库初始化脚本
-- ==========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS app_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE app_config;

-- 创建配置表
DROP TABLE IF EXISTS application_config;
CREATE TABLE application_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(255) NOT NULL COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(100) NOT NULL COMMENT '配置类型(datasource,redis,kafka,business,framework)',
    environment VARCHAR(50) NOT NULL COMMENT '环境(development,test,production)',
    description TEXT COMMENT '配置描述',
    encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密存储',
    required_restart BOOLEAN DEFAULT FALSE COMMENT '是否需要重启应用',
    active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) COMMENT '创建人',
    updated_by VARCHAR(100) COMMENT '更新人',
    
    UNIQUE KEY uk_config_key_env_type (config_key, environment, config_type),
    INDEX idx_config_type_env (config_type, environment),
    INDEX idx_environment (environment),
    INDEX idx_active (active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表';

-- 创建配置历史表
DROP TABLE IF EXISTS config_history;
CREATE TABLE config_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(255) NOT NULL COMMENT '配置键',
    config_type VARCHAR(100) NOT NULL COMMENT '配置类型',
    old_value TEXT COMMENT '旧值',
    new_value TEXT COMMENT '新值',
    environment VARCHAR(50) NOT NULL COMMENT '环境',
    operator_id VARCHAR(100) COMMENT '操作人ID',
    change_reason TEXT COMMENT '变更原因',
    change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    
    INDEX idx_config_key_env (config_key, environment),
    INDEX idx_config_type_env (config_type, environment),
    INDEX idx_operator_id (operator_id),
    INDEX idx_change_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置变更历史表';

-- ==========================================
-- 插入初始配置数据
-- ==========================================

-- 开发环境数据源配置
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('url', 'jdbc:mysql://localhost:3306/business_db_dev', 'datasource', 'development', '开发数据库URL', false, 'system'),
('username', 'dev_user', 'datasource', 'development', '开发数据库用户', false, 'system'),
('password', 'dev_password', 'datasource', 'development', '开发数据库密码', true, 'system'),
('driver-class-name', 'com.mysql.cj.jdbc.Driver', 'datasource', 'development', 'JDBC驱动', false, 'system'),
('maximum-pool-size', '10', 'datasource', 'development', '最大连接池大小', false, 'system'),
('minimum-idle', '3', 'datasource', 'development', '最小空闲连接数', false, 'system'),
('connection-timeout', '30000', 'datasource', 'development', '连接超时时间(ms)', false, 'system'),
('idle-timeout', '600000', 'datasource', 'development', '空闲超时时间(ms)', false, 'system'),
('max-lifetime', '1800000', 'datasource', 'development', '连接最大存活时间(ms)', false, 'system');

-- 生产环境数据源配置
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('url', 'jdbc:mysql://prod-db-cluster:3306/business_db', 'datasource', 'production', '生产数据库URL', false, 'system'),
('username', 'prod_user', 'datasource', 'production', '生产数据库用户', false, 'system'),
('password', 'encrypted_prod_password_here', 'datasource', 'production', '生产数据库密码', true, 'system'),
('driver-class-name', 'com.mysql.cj.jdbc.Driver', 'datasource', 'production', 'JDBC驱动', false, 'system'),
('maximum-pool-size', '20', 'datasource', 'production', '最大连接池大小', false, 'system'),
('minimum-idle', '5', 'datasource', 'production', '最小空闲连接数', false, 'system'),
('connection-timeout', '30000', 'datasource', 'production', '连接超时时间(ms)', false, 'system'),
('idle-timeout', '600000', 'datasource', 'production', '空闲超时时间(ms)', false, 'system'),
('max-lifetime', '1800000', 'datasource', 'production', '连接最大存活时间(ms)', false, 'system');

-- Redis配置 - 开发环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('host', 'localhost', 'redis', 'development', 'Redis主机', false, 'system'),
('port', '6379', 'redis', 'development', 'Redis端口', false, 'system'),
('password', '', 'redis', 'development', 'Redis密码', false, 'system'),
('database', '0', 'redis', 'development', 'Redis数据库', false, 'system'),
('timeout', '2000', 'redis', 'development', '连接超时时间(ms)', false, 'system'),
('lettuce.pool.max-active', '8', 'redis', 'development', '最大活跃连接数', false, 'system'),
('lettuce.pool.max-idle', '8', 'redis', 'development', '最大空闲连接数', false, 'system'),
('lettuce.pool.min-idle', '0', 'redis', 'development', '最小空闲连接数', false, 'system');

-- Redis配置 - 生产环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('host', 'redis-cluster.prod.com', 'redis', 'production', 'Redis主机', false, 'system'),
('port', '6379', 'redis', 'production', 'Redis端口', false, 'system'),
('password', 'encrypted_redis_password', 'redis', 'production', 'Redis密码', true, 'system'),
('database', '0', 'redis', 'production', 'Redis数据库', false, 'system'),
('timeout', '3000', 'redis', 'production', '连接超时时间(ms)', false, 'system'),
('lettuce.pool.max-active', '16', 'redis', 'production', '最大活跃连接数', false, 'system'),
('lettuce.pool.max-idle', '16', 'redis', 'production', '最大空闲连接数', false, 'system'),
('lettuce.pool.min-idle', '2', 'redis', 'production', '最小空闲连接数', false, 'system');

-- Kafka配置 - 开发环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('bootstrap-servers', 'localhost:9092', 'kafka', 'development', 'Kafka集群地址', false, 'system'),
('acks', '1', 'kafka', 'development', '确认机制', false, 'system'),
('retries', '3', 'kafka', 'development', '重试次数', false, 'system'),
('batch-size', '16384', 'kafka', 'development', '批量大小', false, 'system'),
('linger-ms', '5', 'kafka', 'development', '延迟发送时间(ms)', false, 'system'),
('buffer-memory', '33554432', 'kafka', 'development', '缓冲区内存大小', false, 'system');

-- Kafka配置 - 生产环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('bootstrap-servers', 'kafka1:9092,kafka2:9092,kafka3:9092', 'kafka', 'production', 'Kafka集群地址', false, 'system'),
('acks', 'all', 'kafka', 'production', '确认机制', false, 'system'),
('retries', '5', 'kafka', 'production', '重试次数', false, 'system'),
('batch-size', '32768', 'kafka', 'production', '批量大小', false, 'system'),
('linger-ms', '10', 'kafka', 'production', '延迟发送时间(ms)', false, 'system'),
('buffer-memory', '67108864', 'kafka', 'production', '缓冲区内存大小', false, 'system');

-- 业务配置 - 开发环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('app.max-file-size', '5MB', 'business', 'development', '最大文件上传大小', false, 'system'),
('app.session-timeout', '3600', 'business', 'development', '会话超时时间(秒)', false, 'system'),
('app.enable-debug', 'true', 'business', 'development', '调试模式开关', false, 'system'),
('app.api-rate-limit', '500', 'business', 'development', 'API限流阈值', false, 'system'),
('app.cache-ttl', '300', 'business', 'development', '缓存过期时间(秒)', false, 'system'),
('app.batch-size', '100', 'business', 'development', '批处理大小', false, 'system');

-- 业务配置 - 生产环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('app.max-file-size', '10MB', 'business', 'production', '最大文件上传大小', false, 'system'),
('app.session-timeout', '1800', 'business', 'production', '会话超时时间(秒)', false, 'system'),
('app.enable-debug', 'false', 'business', 'production', '调试模式开关', false, 'system'),
('app.api-rate-limit', '1000', 'business', 'production', 'API限流阈值', false, 'system'),
('app.cache-ttl', '600', 'business', 'production', '缓存过期时间(秒)', false, 'system'),
('app.batch-size', '200', 'business', 'production', '批处理大小', false, 'system');

-- 框架配置 - 开发环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('logging.level.root', 'INFO', 'framework', 'development', '根日志级别', false, 'system'),
('logging.level.com.example', 'DEBUG', 'framework', 'development', '应用日志级别', false, 'system'),
('logging.level.org.springframework', 'INFO', 'framework', 'development', 'Spring框架日志级别', false, 'system'),
('logging.level.org.hibernate', 'INFO', 'framework', 'development', 'Hibernate日志级别', false, 'system'),
('server.port', '8080', 'framework', 'development', '服务端口', false, 'system'),
('server.servlet.context-path', '/api', 'framework', 'development', '应用上下文路径', false, 'system'),
('management.endpoints.web.exposure.include', 'health,info,metrics,configprops', 'framework', 'development', 'Actuator端点', false, 'system');

-- 框架配置 - 生产环境
INSERT INTO application_config (config_key, config_value, config_type, environment, description, encrypted, created_by) VALUES 
('logging.level.root', 'WARN', 'framework', 'production', '根日志级别', false, 'system'),
('logging.level.com.example', 'INFO', 'framework', 'production', '应用日志级别', false, 'system'),
('logging.level.org.springframework', 'WARN', 'framework', 'production', 'Spring框架日志级别', false, 'system'),
('logging.level.org.hibernate', 'WARN', 'framework', 'production', 'Hibernate日志级别', false, 'system'),
('server.port', '8080', 'framework', 'production', '服务端口', false, 'system'),
('server.servlet.context-path', '/api', 'framework', 'production', '应用上下文路径', false, 'system'),
('management.endpoints.web.exposure.include', 'health,metrics', 'framework', 'production', 'Actuator端点', false, 'system');

-- ==========================================
-- 创建视图和存储过程
-- ==========================================

-- 创建配置概览视图
CREATE OR REPLACE VIEW v_config_overview AS
SELECT 
    config_type,
    environment,
    COUNT(*) as config_count,
    COUNT(CASE WHEN encrypted = TRUE THEN 1 END) as encrypted_count,
    COUNT(CASE WHEN active = TRUE THEN 1 END) as active_count,
    MAX(updated_at) as last_updated
FROM application_config
GROUP BY config_type, environment;

-- 创建最近变更视图
CREATE OR REPLACE VIEW v_recent_changes AS
SELECT 
    h.config_key,
    h.config_type,
    h.environment,
    h.operator_id,
    h.change_reason,
    h.change_time,
    c.description
FROM config_history h
LEFT JOIN application_config c ON h.config_key = c.config_key 
    AND h.environment = c.environment 
    AND h.config_type = c.config_type
ORDER BY h.change_time DESC
LIMIT 100;

-- 创建存储过程：配置备份
DELIMITER //
CREATE PROCEDURE sp_backup_config(IN env_name VARCHAR(50))
BEGIN
    DECLARE backup_table_name VARCHAR(100);
    SET backup_table_name = CONCAT('config_backup_', env_name, '_', DATE_FORMAT(NOW(), '%Y%m%d_%H%i%s'));
    
    SET @sql = CONCAT('CREATE TABLE ', backup_table_name, ' AS SELECT * FROM application_config WHERE environment = ? AND active = TRUE');
    PREPARE stmt FROM @sql;
    SET @env = env_name;
    EXECUTE stmt USING @env;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('配置备份完成，备份表名：', backup_table_name) as result;
END //
DELIMITER ;

-- ==========================================
-- 创建索引优化查询性能
-- ==========================================

-- 为常用查询创建复合索引
ALTER TABLE application_config ADD INDEX idx_type_env_active (config_type, environment, active);
ALTER TABLE application_config ADD INDEX idx_key_env_active (config_key, environment, active);
ALTER TABLE config_history ADD INDEX idx_key_type_env_time (config_key, config_type, environment, change_time);

-- ==========================================
-- 初始化完成
-- ==========================================
SELECT '数据库初始化完成!' as status;