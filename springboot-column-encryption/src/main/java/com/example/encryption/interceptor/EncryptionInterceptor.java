package com.example.encryption.interceptor;

import com.example.encryption.annotation.Encrypted;
import com.example.encryption.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 加密拦截器
 *
 * 功能：
 * - 拦截 INSERT 和 UPDATE 操作，自动加密 @Encrypted 注解字段
 * - 拦截查询结果，自动解密 @Encrypted 注解字段
 * - 使用缓存提高性能
 */
@Slf4j
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
    ),
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    )
})
public class EncryptionInterceptor implements Interceptor {

    /**
     * 字段加密缓存
     */
    private final Map<String, Boolean> encryptionCache = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];

        // 根据参数数量获取parameter对象
        Object parameter = null;
        if (invocation.getArgs().length > 1) {
            parameter = invocation.getArgs()[1];
        }

        String methodName = invocation.getMethod().getName();

        log.info("🔍 MyBatis拦截器执行: {}, 方法: {}, 参数数量: {}", mappedStatement.getId(), methodName, invocation.getArgs().length);

        // 只处理 update 方法（包括 INSERT、UPDATE、DELETE）
        if ("update".equals(methodName)) {
            // 处理 INSERT/UPDATE 操作的加密
            if (parameter != null) {
                log.info("🔒 MyBatis拦截器处理加密参数: {}", parameter.getClass().getSimpleName());
                encryptParameter(parameter);
                log.info("✅ MyBatis拦截器加密处理完成");
            } else {
                log.debug("🔒 MyBatis拦截器跳过null参数");
            }
        }

        // 继续执行原始操作
        Object result = invocation.proceed();

        // 处理查询结果的解密（只对query方法）
        if ("query".equals(methodName)) {
            if (result != null) {
                log.info("🔓 处理查询结果解密: {}", result.getClass().getSimpleName());

                if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) result;
                    log.info("🔓 解密列表，包含 {} 个元素", list.size());
                    for (Object item : list) {
                        decryptObject(item);
                    }
                } else {
                    decryptObject(result);
                }
            }
        }

        return result;
    }

    /**
     * 加密参数对象中标记了 @Encrypted 注解的字段
     * 注意：这里我们只处理实体对象，不处理单个参数值（单个参数值由 TypeHandler 处理）
     */
    private void encryptParameter(Object parameter) {
        if (parameter == null) {
            return;
        }

        try {
            Class<?> clazz = parameter.getClass();

            // 跳过基本类型、Map、和集合类型 - 这些通常作为查询参数，由 TypeHandler 处理
            if (isBasicType(clazz) || parameter instanceof Map || parameter instanceof java.util.Collection) {
                log.debug("跳过基本类型、Map或集合参数: {}", clazz.getSimpleName());
                return;
            }

            // 只处理实体对象（包含 @Encrypted 注解的类）
            boolean hasEncryptedFields = false;
            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                java.lang.reflect.Field[] fields = currentClass.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if (field.isAnnotationPresent(Encrypted.class)) {
                        hasEncryptedFields = true;
                        break;
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            if (hasEncryptedFields) {
                log.info("🔒 拦截器发现实体对象，开始加密: {}", clazz.getSimpleName());
                encryptFields(parameter, clazz);
            } else {
                log.debug("对象没有加密字段，跳过处理: {}", clazz.getSimpleName());
            }

        } catch (Exception e) {
            log.error("❌ 加密参数失败: {}", parameter.getClass().getSimpleName(), e);
        }
    }

    /**
     * 递归加密对象的字段
     */
    private void encryptFields(Object obj, Class<?> clazz) {
        log.info("🔒 拦截器开始加密对象: {}", clazz.getSimpleName());
        Class<?> currentClass = clazz;
        int encryptedCount = 0;

        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value instanceof String) {
                        String fieldName = field.getName();
                        String cacheKey = clazz.getName() + "." + fieldName;

                        // 检查缓存
                        Boolean shouldEncrypt = encryptionCache.get(cacheKey);
                        if (shouldEncrypt == null) {
                            shouldEncrypt = field.isAnnotationPresent(Encrypted.class);
                            encryptionCache.put(cacheKey, shouldEncrypt);
                            log.debug("字段 {}.{} 加密状态: {}", clazz.getSimpleName(), fieldName, shouldEncrypt);
                        }

                        if (shouldEncrypt) {
                            String stringValue = (String) value;
                            if (stringValue != null && !stringValue.isEmpty() && !CryptoUtil.isEncrypted(stringValue)) {
                                log.info("🔐 拦截器正在加密字段: {}.{} = {}", clazz.getSimpleName(), fieldName, stringValue);
                                String encryptedValue = CryptoUtil.encrypt(stringValue);
                                field.set(obj, encryptedValue);
                                encryptedCount++;
                                log.info("✅ 拦截器加密完成: {}.{} -> {}", clazz.getSimpleName(), fieldName, encryptedValue.substring(0, Math.min(20, encryptedValue.length())) + "...");
                            } else if (stringValue != null && stringValue.isEmpty()) {
                                log.debug("跳过空字段: {}.{}", clazz.getSimpleName(), fieldName);
                            } else if (stringValue != null && CryptoUtil.isEncrypted(stringValue)) {
                                log.debug("字段已加密，跳过: {}.{}", clazz.getSimpleName(), fieldName);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ 拦截器处理字段失败: {}", field.getName(), e);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        log.info("🎉 拦截器对象加密完成: {}, 共加密 {} 个字段", clazz.getSimpleName(), encryptedCount);
    }

    /**
     * 解密对象中标记了 @Encrypted 注解的字段
     */
    private void decryptObject(Object obj) {
        if (obj == null) {
            return;
        }

        try {
            Class<?> clazz = obj.getClass();

            // 跳过基本类型和Map
            if (isBasicType(clazz) || obj instanceof Map) {
                return;
            }

            // 递归处理字段
            decryptFields(obj, clazz);

        } catch (Exception e) {
            log.error("解密对象失败: {}", obj.getClass().getSimpleName(), e);
        }
    }

    /**
     * 递归解密对象的字段
     */
    private void decryptFields(Object obj, Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value instanceof String) {
                        String fieldName = field.getName();
                        String cacheKey = clazz.getName() + "." + fieldName;

                        // 检查缓存
                        Boolean shouldEncrypt = encryptionCache.get(cacheKey);
                        if (shouldEncrypt == null) {
                            shouldEncrypt = field.isAnnotationPresent(Encrypted.class);
                            encryptionCache.put(cacheKey, shouldEncrypt);
                        }

                        if (shouldEncrypt) {
                            String stringValue = (String) value;
                            if (stringValue != null && !stringValue.isEmpty() && CryptoUtil.isEncrypted(stringValue)) {
                                String decryptedValue = CryptoUtil.decrypt(stringValue);
                                field.set(obj, decryptedValue);
                                log.debug("解密字段: {}.{} -> {}", clazz.getSimpleName(), fieldName, decryptedValue.substring(0, Math.min(10, decryptedValue.length())));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("处理字段失败: {}", field.getName(), e);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * 检查是否为基本类型
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               Number.class.isAssignableFrom(clazz) ||
               clazz == Boolean.class ||
               clazz == Character.class;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 初始化属性
    }
}