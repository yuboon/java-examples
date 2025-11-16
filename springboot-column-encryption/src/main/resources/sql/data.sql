-- 示例数据（插入前会自动加密，查询时会自动解密）
-- 这些数据展示 @Encrypted 注解的透明加解密效果

-- 清空现有数据
DELETE FROM users;

-- 插入初始化示例用户数据
INSERT INTO users (username, phone, id_card, email, bank_card, address, age, gender, occupation, enabled, remark) VALUES
('数据库初始用户', '13899990001', '110101199009099999', 'db.init@example.com', '6222021234567899999', '北京市海淀区中关村大街1号', 35, '男', '系统管理员', TRUE, '数据库初始化用户 - 展示加密效果'),
('示例用户小明', '13899990002', '110101199010101010', 'xiaoming@example.com', '6222021234567898888', '上海市浦东新区世纪大道200号', 26, '男', 'Java开发工程师', TRUE, '数据库初始化用户 - 展示加密效果'),
('示例用户小红', '13899990003', '110101199011111111', 'xiaohong@example.com', '6222021234567897777', '广州市天河区珠江新城100号', 24, '女', '前端开发工程师', TRUE, '数据库初始化用户 - 展示加密效果');

-- 查询确认数据插入
SELECT
    id,
    username,
    phone AS encrypted_phone,
    id_card AS encrypted_id_card,
    email AS encrypted_email,
    bank_card AS encrypted_bank_card,
    address AS encrypted_address,
    age,
    gender,
    occupation,
    enabled,
    remark,
    create_time
FROM users
ORDER BY create_time;

-- 注意：上面的查询结果中，phone, id_card, email, bank_card, address 字段显示的是加密后的密文
-- 当通过 MyBatis 查询时，这些字段会自动解密为明文返回给应用层