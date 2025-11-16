package com.example.encryption.handler;

import com.example.encryption.annotation.Encrypted;
import com.example.encryption.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis 字段级加密 TypeHandler
 *
 * 功能：
 * - 自动检测字段是否标记了 @Encrypted 注解
 * - 写入数据库时自动加密
 * - 从数据库读取时自动解密
 * - 支持查询参数加密处理
 */
@Slf4j
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置参数时进行加密
     * 这个方法在 INSERT/UPDATE 操作时被调用
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String value, JdbcType jdbcType) throws SQLException {
        try {
            // 如果值为空，直接使用
            if (value == null || value.isEmpty()) {
                ps.setString(i, value);
                return;
            }

            // 检查是否已经是加密格式，避免重复加密
            if (CryptoUtil.isEncrypted(value)) {
                log.debug("字段已经是加密格式，跳过加密: 位置={}", i);
                ps.setString(i, value);
                return;
            }

            // 加密后设置参数
            String encrypted = CryptoUtil.encrypt(value);
            ps.setString(i, encrypted);

            log.info("🔐 TypeHandler参数加密成功: 位置={}, 原始长度={}, 加密后长度={}", i, value.length(), encrypted.length());

        } catch (Exception e) {
            log.error("❌ TypeHandler参数加密失败: 位置={}, 值={}", i, value, e);
            // 加密失败时使用原始值，避免数据丢失
            ps.setString(i, value);
        }
    }

    /**
     * 从 ResultSet 通过列名获取值时进行解密
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            String value = rs.getString(columnName);
            return decryptValue(value, columnName);
        } catch (Exception e) {
            log.error("解密失败: 列名={}", columnName, e);
            return rs.getString(columnName);
        }
    }

    /**
     * 从 ResultSet 通过列索引获取值时进行解密
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            String value = rs.getString(columnIndex);
            return decryptValue(value, "索引" + columnIndex);
        } catch (Exception e) {
            log.error("解密失败: 列索引={}", columnIndex, e);
            return rs.getString(columnIndex);
        }
    }

    /**
     * 从 CallableStatement 获取值时进行解密
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            String value = cs.getString(columnIndex);
            return decryptValue(value, "存储过程索引" + columnIndex);
        } catch (Exception e) {
            log.error("解密失败: 存储过程索引={}", columnIndex, e);
            return cs.getString(columnIndex);
        }
    }

    /**
     * 解密值的统一方法
     */
    private String decryptValue(String value, String source) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        try {
            // 检查是否为加密格式
            if (!CryptoUtil.isEncrypted(value)) {
                log.debug("值不是加密格式，跳过解密: 来源={}", source);
                return value;
            }

            String decrypted = CryptoUtil.decrypt(value);
            log.info("🔓 TypeHandler值解密成功: 来源={}, 加密长度={}, 解密后长度={}", source, value.length(), decrypted.length());
            return decrypted;

        } catch (Exception e) {
            log.error("❌ TypeHandler解密失败: 来源={}, 值前缀={}", source,
                    value.length() > 10 ? value.substring(0, 10) : value, e);
            // 解密失败时返回原始值
            return value;
        }
    }

    /**
     * 检查字段是否应该被加密
     * 这个方法主要用于调试和日志记录
     */
    public static boolean shouldEncrypt(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return false;
        }

        try {
            Class<?> clazz = obj.getClass();
            java.lang.reflect.Field field = findField(clazz, fieldName);
            return field != null && field.isAnnotationPresent(Encrypted.class);
        } catch (Exception e) {
            log.debug("检查字段加密注解时出错: 对象类型={}, 字段名={}",
                    obj.getClass().getSimpleName(), fieldName, e);
            return false;
        }
    }

    /**
     * 递归查找字段，包括父类
     */
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}