-- 测试数据创建脚本
-- 用于轻量级BI报表平台演示

-- 创建测试数据库，指定字符集
CREATE DATABASE IF NOT EXISTS test_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE test_db;

-- 创建销售数据表，指定字符集
CREATE TABLE sales_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    region VARCHAR(50) NOT NULL COMMENT '销售区域',
    product_category VARCHAR(50) NOT NULL COMMENT '产品分类',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    sales_amount DECIMAL(10,2) NOT NULL COMMENT '销售金额',
    quantity INT NOT NULL COMMENT '销售数量',
    sale_date DATE NOT NULL COMMENT '销售日期',
    sales_person VARCHAR(50) NOT NULL COMMENT '销售人员',
    customer_type VARCHAR(30) NOT NULL COMMENT '客户类型',
    payment_method VARCHAR(30) NOT NULL COMMENT '支付方式'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO sales_data (region, product_category, product_name, sales_amount, quantity, sale_date, sales_person, customer_type, payment_method) VALUES
-- 华北区域数据
('华北', '电子产品', 'iPhone 15', 6999.00, 8, '2024-01-15', '张三', '个人', '支付宝'),
('华北', '电子产品', 'MacBook Pro', 12999.00, 3, '2024-01-16', '张三', '企业', '银行转账'),
('华北', '电子产品', 'iPad Air', 4799.00, 5, '2024-01-17', '李四', '个人', '微信支付'),
('华北', '服装', '商务西装', 1299.00, 12, '2024-01-18', '李四', '个人', '信用卡'),
('华北', '服装', '休闲T恤', 199.00, 30, '2024-01-19', '王五', '个人', '现金'),
('华北', '食品', '进口巧克力', 89.00, 45, '2024-01-20', '王五', '个人', '支付宝'),

-- 华东区域数据
('华东', '电子产品', 'iPhone 15', 6899.00, 12, '2024-01-21', '赵六', '个人', '微信支付'),
('华东', '电子产品', '小米笔记本', 4999.00, 8, '2024-01-22', '赵六', '企业', '银行转账'),
('华东', '电子产品', 'AirPods', 1299.00, 15, '2024-01-23', '钱七', '个人', '支付宝'),
('华东', '服装', '连衣裙', 599.00, 25, '2024-01-24', '钱七', '个人', '信用卡'),
('华东', '服装', '运动鞋', 899.00, 18, '2024-01-25', '孙八', '个人', '微信支付'),
('华东', '食品', '有机蔬菜', 45.00, 60, '2024-01-26', '孙八', '个人', '现金'),

-- 华南区域数据
('华南', '电子产品', '华为手机', 5499.00, 10, '2024-01-27', '周九', '个人', '支付宝'),
('华南', '电子产品', '联想电脑', 6299.00, 6, '2024-01-28', '周九', '企业', '银行转账'),
('华南', '电子产品', '智能手表', 1999.00, 20, '2024-01-29', '吴十', '个人', '微信支付'),
('华南', '服装', '防晒衣', 299.00, 35, '2024-01-30', '吴十', '个人', '信用卡'),
('华南', '服装', '凉鞋', 399.00, 22, '2024-01-31', '郑一', '个人', '支付宝'),
('华南', '食品', '热带水果', 68.00, 80, '2024-02-01', '郑一', '个人', '现金'),

-- 华西区域数据
('华西', '电子产品', 'OPPO手机', 2999.00, 15, '2024-02-02', '王二', '个人', '微信支付'),
('华西', '电子产品', '蓝牙音箱', 399.00, 25, '2024-02-03', '王二', '个人', '支付宝'),
('华西', '电子产品', '移动硬盘', 599.00, 18, '2024-02-04', '李三', '企业', '银行转账'),
('华西', '服装', '羽绒服', 899.00, 28, '2024-02-05', '李三', '个人', '信用卡'),
('华西', '服装', '保暖内衣', 199.00, 40, '2024-02-06', '赵四', '个人', '现金'),
('华西', '食品', '牛肉干', 128.00, 55, '2024-02-07', '赵四', '个人', '支付宝'),

-- 添加更多历史数据用于时间序列分析
('华北', '电子产品', 'iPhone 15', 6799.00, 6, '2024-02-08', '张三', '个人', '微信支付'),
('华北', '电子产品', 'MacBook Pro', 12499.00, 2, '2024-02-09', '张三', '企业', '银行转账'),
('华东', '电子产品', 'iPhone 15', 7099.00, 9, '2024-02-10', '赵六', '个人', '支付宝'),
('华东', '服装', '连衣裙', 579.00, 20, '2024-02-11', '钱七', '个人', '信用卡'),
('华南', '电子产品', '华为手机', 5399.00, 8, '2024-02-12', '周九', '个人', '微信支付'),
('华西', '电子产品', 'OPPO手机', 2899.00, 12, '2024-02-13', '王二', '个人', '支付宝');

-- 创建用户信息表用于多表关联演示
CREATE TABLE user_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL COMMENT '用户名',
    region VARCHAR(50) NOT NULL COMMENT '所属区域',
    level VARCHAR(20) NOT NULL COMMENT '用户等级',
    register_date DATE NOT NULL COMMENT '注册日期'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO user_info (user_name, region, level, register_date) VALUES
('张三', '华北', 'VIP', '2023-06-15'),
('李四', '华北', '普通', '2023-08-20'),
('王五', '华北', 'VIP', '2023-09-10'),
('赵六', '华东', '金牌', '2023-07-05'),
('钱七', '华东', '普通', '2023-10-12'),
('孙八', '华东', 'VIP', '2023-11-03'),
('周九', '华南', '金牌', '2023-05-28'),
('吴十', '华南', '普通', '2023-09-15'),
('郑一', '华南', 'VIP', '2023-12-01'),
('王二', '华西', '普通', '2023-08-10'),
('李三', '华西', '金牌', '2023-10-20'),
('赵四', '华西', 'VIP', '2023-11-15');

-- 创建产品信息表
CREATE TABLE product_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    category VARCHAR(50) NOT NULL COMMENT '产品分类',
    brand VARCHAR(50) NOT NULL COMMENT '品牌',
    cost_price DECIMAL(10,2) NOT NULL COMMENT '成本价',
    created_date DATE NOT NULL COMMENT '创建日期'
)CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO product_info (product_name, category, brand, cost_price, created_date) VALUES
('iPhone 15', '电子产品', 'Apple', 4500.00, '2023-09-15'),
('MacBook Pro', '电子产品', 'Apple', 8000.00, '2023-06-20'),
('iPad Air', '电子产品', 'Apple', 2800.00, '2023-08-10'),
('AirPods', '电子产品', 'Apple', 800.00, '2023-07-12'),
('华为手机', '电子产品', '华为', 3500.00, '2023-09-01'),
('小米笔记本', '电子产品', '小米', 3200.00, '2023-05-15'),
('联想电脑', '电子产品', '联想', 4200.00, '2023-08-25'),
('OPPO手机', '电子产品', 'OPPO', 1800.00, '2023-10-10'),
('蓝牙音箱', '电子产品', '小米', 200.00, '2023-11-05'),
('智能手表', '电子产品', '华为', 1200.00, '2023-09-20'),
('移动硬盘', '电子产品', '西部数据', 350.00, '2023-07-18'),
('商务西装', '服装', '雅戈尔', 800.00, '2023-04-12'),
('休闲T恤', '服装', '优衣库', 80.00, '2023-06-25'),
('连衣裙', '服装', 'ZARA', 350.00, '2023-08-15'),
('运动鞋', '服装', '耐克', 550.00, '2023-09-10'),
('防晒衣', '服装', '迪卡侬', 150.00, '2023-10-20'),
('凉鞋', '服装', '百丽', 220.00, '2023-11-12'),
('羽绒服', '服装', '波司登', 550.00, '2023-12-05'),
('保暖内衣', '服装', '三枪', 120.00, '2023-10-15'),
('进口巧克力', '食品', '费列罗', 60.00, '2023-08-20'),
('有机蔬菜', '食品', '有机农场', 25.00, '2023-09-01'),
('热带水果', '食品', '果园直供', 45.00, '2023-10-10'),
('牛肉干', '食品', '草原牧歌', 85.00, '2023-11-20');

-- 创建视图用于统计查询
CREATE VIEW sales_summary AS
SELECT
    region,
    product_category,
    COUNT(*) as order_count,
    SUM(quantity) as total_quantity,
    SUM(sales_amount) as total_sales,
    AVG(sales_amount) as avg_order_amount,
    MIN(sales_amount) as min_order_amount,
    MAX(sales_amount) as max_order_amount
FROM sales_data
GROUP BY region, product_category;

-- 添加索引提高查询性能
CREATE INDEX idx_sales_region ON sales_data(region);
CREATE INDEX idx_sales_category ON sales_data(product_category);
CREATE INDEX idx_sales_date ON sales_data(sale_date);
CREATE INDEX idx_sales_person ON sales_data(sales_person);

-- 查询测试数据
SELECT '数据库初始化完成' as message;
SELECT COUNT(*) as total_records FROM sales_data;
SELECT DISTINCT region FROM sales_data ORDER BY region;
SELECT DISTINCT product_category FROM sales_data ORDER BY product_category;