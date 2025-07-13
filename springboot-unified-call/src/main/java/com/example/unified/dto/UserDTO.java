package com.example.unified.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户数据传输对象，用于服务间数据传递
 */
@Data
public class UserDTO implements Serializable {
    private Long id;             // 用户ID
    private String username;     // 用户名
    private String email;        // 邮箱
    private Integer age;         // 年龄
    private String status;       // 状态（ACTIVE/INACTIVE）
}