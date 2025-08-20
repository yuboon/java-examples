-- 插入默认防火墙规则
INSERT INTO firewall_rule (rule_name, api_pattern, qps_limit, user_limit, time_window, enabled, description) VALUES
('默认API限流', '/api/test/**', 1, 60, 60, true, '默认API接口限流规则'),
('用户登录限流', '/api/auth/login', 10, 5, 300, true, '用户登录接口限流，防止暴力破解'),
('订单接口限流', '/api/order/**', 50, 20, 60, true, '订单相关接口限流'),
('支付接口限流', '/api/payment/**', 20, 10, 60, true, '支付相关接口限流'),
('文件上传限流', '/api/upload/**', 30, 10, 60, true, '文件上传接口限流'),
('数据导出限流', '/api/export/**', 5, 2, 300, true, '数据导出接口限流，防止大量导出');

-- 插入示例白名单IP
INSERT INTO firewall_whitelist (ip_address, description, enabled) VALUES
('127.0.0.1', '本地回环地址', true),
('::1', 'IPv6本地回环地址', true),
('192.168.1.100', '管理员IP地址', true);

-- 插入示例黑名单IP
INSERT INTO firewall_blacklist (ip_address, reason, expire_time, enabled) VALUES
('192.168.1.200', '恶意攻击IP', NULL, true),
('10.0.0.100', '异常访问行为', DATEADD('HOUR', 24, CURRENT_TIMESTAMP), true);

-- 插入示例统计数据（最近7天的数据）
INSERT INTO firewall_statistics (stat_date, api_path, total_requests, blocked_requests, avg_response_time) VALUES
-- 今天
(CURRENT_DATE, '/api/user/info', 1500, 50, 120.5),
(CURRENT_DATE, '/api/auth/login', 800, 200, 250.8),
(CURRENT_DATE, '/api/order/list', 1200, 30, 180.2),
(CURRENT_DATE, '/api/payment/create', 300, 15, 350.6),
-- 昨天
(DATEADD('DAY', -1, CURRENT_DATE), '/api/user/info', 1350, 45, 115.2),
(DATEADD('DAY', -1, CURRENT_DATE), '/api/auth/login', 750, 180, 245.3),
(DATEADD('DAY', -1, CURRENT_DATE), '/api/order/list', 1100, 25, 175.8),
(DATEADD('DAY', -1, CURRENT_DATE), '/api/payment/create', 280, 12, 340.1),
-- 2天前
(DATEADD('DAY', -2, CURRENT_DATE), '/api/user/info', 1600, 60, 125.8),
(DATEADD('DAY', -2, CURRENT_DATE), '/api/auth/login', 900, 220, 260.5),
(DATEADD('DAY', -2, CURRENT_DATE), '/api/order/list', 1300, 35, 185.4),
(DATEADD('DAY', -2, CURRENT_DATE), '/api/payment/create', 320, 18, 365.2),
-- 3天前
(DATEADD('DAY', -3, CURRENT_DATE), '/api/user/info', 1200, 40, 118.7),
(DATEADD('DAY', -3, CURRENT_DATE), '/api/auth/login', 650, 150, 235.9),
(DATEADD('DAY', -3, CURRENT_DATE), '/api/order/list', 1000, 20, 170.3),
(DATEADD('DAY', -3, CURRENT_DATE), '/api/payment/create', 250, 10, 330.8),
-- 4天前
(DATEADD('DAY', -4, CURRENT_DATE), '/api/user/info', 1400, 55, 122.3),
(DATEADD('DAY', -4, CURRENT_DATE), '/api/auth/login', 820, 190, 255.1),
(DATEADD('DAY', -4, CURRENT_DATE), '/api/order/list', 1150, 28, 178.9),
(DATEADD('DAY', -4, CURRENT_DATE), '/api/payment/create', 290, 14, 345.7),
-- 5天前
(DATEADD('DAY', -5, CURRENT_DATE), '/api/user/info', 1100, 35, 112.9),
(DATEADD('DAY', -5, CURRENT_DATE), '/api/auth/login', 600, 140, 230.4),
(DATEADD('DAY', -5, CURRENT_DATE), '/api/order/list', 950, 18, 165.6),
(DATEADD('DAY', -5, CURRENT_DATE), '/api/payment/create', 220, 8, 325.3),
-- 6天前
(DATEADD('DAY', -6, CURRENT_DATE), '/api/user/info', 1300, 48, 119.6),
(DATEADD('DAY', -6, CURRENT_DATE), '/api/auth/login', 720, 170, 248.7),
(DATEADD('DAY', -6, CURRENT_DATE), '/api/order/list', 1080, 22, 172.1),
(DATEADD('DAY', -6, CURRENT_DATE), '/api/payment/create', 260, 11, 338.9);

-- 插入一些访问日志数据
INSERT INTO firewall_access_log (ip_address, api_path, user_agent, request_method, status_code, block_reason, request_time, response_time) VALUES
('192.168.1.100', '/api/user/info', 'Mozilla/5.0', 'GET', 200, NULL, CURRENT_TIMESTAMP, 120),
('192.168.1.101', '/api/auth/login', 'Chrome/91.0', 'POST', 429, 'QPS限制', CURRENT_TIMESTAMP, 50),
('192.168.1.102', '/api/order/list', 'Safari/14.0', 'GET', 200, NULL, CURRENT_TIMESTAMP, 180),
('192.168.1.103', '/api/payment/create', 'Firefox/89.0', 'POST', 403, 'IP黑名单', CURRENT_TIMESTAMP, 30),
('192.168.1.104', '/api/user/info', 'Edge/91.0', 'GET', 200, NULL, DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), 115),
('192.168.1.105', '/api/auth/login', 'Mozilla/5.0', 'POST', 429, '用户限制', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 80);