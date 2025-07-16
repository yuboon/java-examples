package com.example.model;

import com.example.interceptor.Sensitive;
import com.example.interceptor.SensitiveType;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    
    @Sensitive(type = SensitiveType.PHONE)  // 手机号脱敏
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD)  // 身份证号脱敏
    private String idCard;
}