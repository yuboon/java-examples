package com.example.encryption.entity;

import com.example.encryption.annotation.Encrypted;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 包含需要加密的敏感字段：
 * - phone: 手机号
 * - idCard: 身份证号
 * - email: 邮箱
 * - bankCard: 银行卡号
 * - address: 家庭住址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户ID（主键）
     */
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50字符之间")
    private String username;

    /**
     * 手机号（加密字段）
     */
    @Encrypted
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 身份证号（加密字段）
     */
    @Encrypted
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    /**
     * 邮箱（加密字段）
     */
    @Encrypted
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 银行卡号（加密字段）
     */
    @Encrypted
    @Pattern(regexp = "^\\d{16,19}$", message = "银行卡号格式不正确")
    private String bankCard;

    /**
     * 家庭住址（加密字段）
     */
    @Encrypted
    @Size(max = 200, message = "地址长度不能超过200字符")
    private String address;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别
     */
    private String gender;

    /**
     * 职业
     */
    private String occupation;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 重载toString方法，避免敏感信息泄露
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", phone='" + maskPhone(phone) + '\'' +
                ", idCard='" + maskIdCard(idCard) + '\'' +
                ", email='" + maskEmail(email) + '\'' +
                ", bankCard='" + maskBankCard(bankCard) + '\'' +
                ", address='" + maskAddress(address) + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", occupation='" + occupation + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", enabled=" + enabled +
                ", remark='" + remark + '\'' +
                '}';
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 身份证号脱敏
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);

        if (prefix.length() <= 3) {
            return prefix.charAt(0) + "***" + suffix;
        }
        return prefix.substring(0, 3) + "***" + suffix;
    }

    /**
     * 银行卡号脱敏
     */
    private String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 地址脱敏
     */
    private String maskAddress(String address) {
        if (address == null || address.length() <= 10) {
            return address;
        }
        return address.substring(0, 6) + "******";
    }
}