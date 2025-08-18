-- SQL调用树可视化系统示例数据
-- 插入测试数据用于演示SQL调用树功能

-- 插入示例用户数据
INSERT INTO users (username, email, password) VALUES
('admin', 'admin@example.com', 'admin123'),
('user1', 'user1@example.com', 'user123'),
('user2', 'user2@example.com', 'user123'),
('user3', 'user3@example.com', 'user123'),
('user4', 'user4@example.com', 'user123');

-- 插入示例产品数据
INSERT INTO products (name, description, price, stock, category) VALUES
('笔记本电脑', '高性能笔记本电脑', 5999.00, 50, '电子产品'),
('无线鼠标', '蓝牙无线鼠标', 99.00, 200, '电子产品'),
('机械键盘', '青轴机械键盘', 299.00, 100, '电子产品'),
('显示器', '27寸4K显示器', 1999.00, 30, '电子产品'),
('耳机', '降噪蓝牙耳机', 599.00, 80, '电子产品'),
('手机', '智能手机', 3999.00, 60, '电子产品'),
('平板电脑', '10寸平板电脑', 2499.00, 40, '电子产品'),
('音响', '蓝牙音响', 199.00, 120, '电子产品');

-- 插入示例订单数据
INSERT INTO orders (user_id, order_no, total_amount, status) VALUES
(1, 'ORD001', 6598.00, 'COMPLETED'),
(2, 'ORD002', 398.00, 'COMPLETED'),
(3, 'ORD003', 7997.00, 'PENDING'),
(4, 'ORD004', 798.00, 'COMPLETED'),
(5, 'ORD005', 2698.00, 'SHIPPING');

-- 插入示例订单详情数据
INSERT INTO order_items (order_id, product_name, quantity, price) VALUES
-- 订单1的商品
(1, '笔记本电脑', 1, 5999.00),
(1, '无线鼠标', 1, 99.00),
(1, '机械键盘', 1, 299.00),
(1, '耳机', 1, 599.00),
-- 订单2的商品
(2, '无线鼠标', 2, 99.00),
(2, '音响', 1, 199.00),
-- 订单3的商品
(3, '笔记本电脑', 1, 5999.00),
(3, '显示器', 1, 1999.00),
-- 订单4的商品
(4, '耳机', 1, 599.00),
(4, '音响', 1, 199.00),
-- 订单5的商品
(5, '平板电脑', 1, 2499.00),
(5, '音响', 1, 199.00);