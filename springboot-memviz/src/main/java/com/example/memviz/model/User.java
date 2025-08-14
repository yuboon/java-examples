package com.example.memviz.model;

import cn.hutool.core.util.RandomUtil;

/**
 * 测试深度大小计算的User类
 */
public class User {
    private String name;
    private int age;
    
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // 静态创建一些测试用户，用于验证深度大小计算
    public static void createTestUsers() {
        // 创建几个有大字符串的User对象
        for (int i = 0; i < 5; i++) {
            String largeName = RandomUtil.randomString(200 * 1024); // 200KB的name
            User user = new User(largeName, 25 + i);
            
            // 将User对象存储在某个地方，避免被GC
            TestUserStorage.users.add(user);
        }
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
}