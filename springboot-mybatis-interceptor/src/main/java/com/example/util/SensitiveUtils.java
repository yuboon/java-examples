package com.example.util;

public class SensitiveUtils {
    // 手机号脱敏：保留前3位和后4位
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;  // 非手机号格式不处理
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
    
    // 身份证号脱敏：保留最后2位
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;  // 非身份证格式不处理
        }
        return idCard.replaceAll("\\d{16}(\\d{2})", "****************$1");
    }
}