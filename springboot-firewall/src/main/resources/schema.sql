-- 防火墙规则表
CREATE TABLE IF NOT EXISTS firewall_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    api_pattern VARCHAR(200) NOT NULL COMMENT 'API路径匹配模式',
    qps_limit INT DEFAULT 100 COMMENT 'QPS限制',
    user_limit INT DEFAULT 60 COMMENT '单用户时间窗限制(分钟)',
    time_window INT DEFAULT 60 COMMENT '时间窗口(秒)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    description VARCHAR(500) COMMENT '规则描述',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 黑名单表
CREATE TABLE IF NOT EXISTS firewall_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    reason VARCHAR(200) COMMENT '封禁原因',
    expire_time TIMESTAMP COMMENT '过期时间(NULL表示永久)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_ip UNIQUE (ip_address)
);

-- 白名单表
CREATE TABLE IF NOT EXISTS firewall_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    description VARCHAR(200) COMMENT '描述',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_whitelist_ip UNIQUE (ip_address)
);

-- 访问日志表
CREATE TABLE IF NOT EXISTS firewall_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    api_path VARCHAR(200) NOT NULL COMMENT 'API路径',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    request_method VARCHAR(10) COMMENT '请求方法',
    status_code INT COMMENT '响应状态码',
    block_reason VARCHAR(100) COMMENT '拦截原因',
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    response_time BIGINT COMMENT '响应时间(毫秒)'
);

-- 为访问日志表创建索引
CREATE INDEX IF NOT EXISTS idx_ip_time ON firewall_access_log (ip_address, request_time);
CREATE INDEX IF NOT EXISTS idx_api_time ON firewall_access_log (api_path, request_time);

-- 统计表
CREATE TABLE IF NOT EXISTS firewall_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    api_path VARCHAR(200) NOT NULL COMMENT 'API路径',
    total_requests BIGINT DEFAULT 0 COMMENT '总请求数',
    blocked_requests BIGINT DEFAULT 0 COMMENT '被拦截请求数',
    avg_response_time DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_date_api UNIQUE (stat_date, api_path)
);