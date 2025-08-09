-- 服务器配置表
CREATE TABLE IF NOT EXISTS servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '服务器名称',
    host VARCHAR(255) NOT NULL COMMENT '服务器地址',
    port INT DEFAULT 22 COMMENT 'SSH端口',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(500) NOT NULL COMMENT '密码（建议加密存储）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 删除现有测试数据（避免重复插入）
DELETE FROM servers;

-- 插入测试服务器数据
INSERT INTO servers (name, host, port, username, password) VALUES
('本地测试服务器', 'localhost', 22, 'root', 'password'),
('开发服务器', '192.168.1.100', 22, 'dev', 'devpass'),
('测试服务器', '192.168.1.101', 22, 'test', 'testpass'),
('生产服务器', '192.168.1.200', 22, 'prod', 'prodpass');