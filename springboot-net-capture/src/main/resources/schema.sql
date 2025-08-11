-- 数据包信息表
CREATE TABLE IF NOT EXISTS packet_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    capture_time TIMESTAMP NOT NULL,
    source_ip VARCHAR(45),
    destination_ip VARCHAR(45),
    source_port INTEGER,
    destination_port INTEGER,
    protocol VARCHAR(20),
    packet_length INTEGER,
    payload CLOB,
    http_method VARCHAR(10),
    http_url VARCHAR(500),
    http_headers CLOB,
    http_body CLOB,
    http_status INTEGER,
    tcp_seq_number BIGINT,
    tcp_ack_number BIGINT,
    tcp_flags VARCHAR(1000),
    network_interface VARCHAR(100)
);

-- 创建索引（H2数据库语法）
CREATE INDEX IF NOT EXISTS idx_capture_time ON packet_info(capture_time);
CREATE INDEX IF NOT EXISTS idx_protocol ON packet_info(protocol);
CREATE INDEX IF NOT EXISTS idx_source_ip ON packet_info(source_ip);
CREATE INDEX IF NOT EXISTS idx_destination_ip ON packet_info(destination_ip);

-- 流量统计表
CREATE TABLE IF NOT EXISTS traffic_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    statistics_time TIMESTAMP NOT NULL,
    time_window VARCHAR(10),
    total_packets BIGINT DEFAULT 0,
    total_bytes BIGINT DEFAULT 0,
    http_packets BIGINT DEFAULT 0,
    tcp_packets BIGINT DEFAULT 0,
    udp_packets BIGINT DEFAULT 0,
    icmp_packets BIGINT DEFAULT 0,
    top_source_ip VARCHAR(45),
    top_destination_ip VARCHAR(45),
    top_source_port INTEGER,
    top_destination_port INTEGER,
    average_packet_size DOUBLE DEFAULT 0
);

-- 创建索引（H2数据库语法）
CREATE INDEX IF NOT EXISTS idx_statistics_time ON traffic_statistics(statistics_time);
CREATE INDEX IF NOT EXISTS idx_time_window ON traffic_statistics(time_window);