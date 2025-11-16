-- 用户表结构 (H2数据库兼容版本)
-- 注意：加密字段在数据库中存储为 VARCHAR 类型，应用层会自动进行加解密处理

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    phone VARCHAR(500),
    id_card VARCHAR(500),
    email VARCHAR(500),
    bank_card VARCHAR(500),
    address VARCHAR(500),
    age INT,
    gender VARCHAR(10),
    occupation VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_create_time ON users(create_time);