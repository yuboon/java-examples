-- schema.sql
CREATE TABLE IF NOT EXISTS danmaku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(255) NOT NULL,
    color VARCHAR(20) DEFAULT '#ffffff',
    font_size INT DEFAULT 24,
    time DOUBLE NOT NULL,
    video_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加一些测试数据
INSERT INTO danmaku (content, color, font_size, time, video_id, user_id, username, created_at)
VALUES
('这是第一条测试弹幕', '#ffffff', 24, 1.0, 'video123', 'user1', '测试用户1', CURRENT_TIMESTAMP),
('这是第二条测试弹幕', '#ff0000', 24, 3.0, 'video123', 'user2', '测试用户2', CURRENT_TIMESTAMP),
('这是第三条测试弹幕', '#00ff00', 24, 5.0, 'video123', 'user3', '测试用户3', CURRENT_TIMESTAMP),
('这是第四条测试弹幕', '#0000ff', 24, 7.0, 'video123', 'user4', '测试用户4', CURRENT_TIMESTAMP);