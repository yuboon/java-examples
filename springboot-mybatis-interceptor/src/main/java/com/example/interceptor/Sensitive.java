package com.example.interceptor;

import java.lang.annotation.*;

// 作用在字段上
@Target(ElementType.FIELD)
// 运行时生效
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    // 脱敏类型（手机号、身份证号等）
    SensitiveType type();
}
