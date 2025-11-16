package com.example.encryption.config;

import com.example.encryption.handler.EncryptTypeHandler;
import com.example.encryption.interceptor.EncryptionInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 字段级加密自动配置类
 *
 * 功能：
 * - 自动注册加密相关的组件到 MyBatis
 * - 配置 ObjectWrapperFactory
 * - 配置 TypeHandler
 * - 提供开关控制
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(name = "encryption.enabled", havingValue = "true", matchIfMissing = true)
public class EncryptionAutoConfiguration {

    /**
     * 注册加密拦截器
     */
    @Bean
    public EncryptionInterceptor encryptionInterceptor() {
        return new EncryptionInterceptor();
    }

    /**
     * 注册 MyBatis ConfigurationCustomizer
     * 用于配置加密相关的组件
     */
    @Bean
    public ConfigurationCustomizer encryptionConfigurationCustomizer(EncryptionInterceptor encryptionInterceptor) {
        return new EncryptionConfigurationCustomizer(encryptionInterceptor);
    }

    /**
     * 加密配置自定义器
     */
    public static class EncryptionConfigurationCustomizer implements ConfigurationCustomizer {

        private final EncryptionInterceptor encryptionInterceptor;

        public EncryptionConfigurationCustomizer(EncryptionInterceptor encryptionInterceptor) {
            this.encryptionInterceptor = encryptionInterceptor;
        }

        @Override
        public void customize(Configuration configuration) {
            log.info("🔐 开始配置 MyBatis 字段级加密功能");

            // 注册加密拦截器（主要加密机制）
            configuration.addInterceptor(encryptionInterceptor);
            log.info("✅ 已注册加密拦截器 - 主要加密机制");

            // 暂时不注册 TypeHandler，避免与拦截器冲突
            // 拦截器会处理所有实体对象的加密
            log.info("⚠️  TypeHandler 已禁用，使用拦截器统一处理加密");

            log.info("🎉 MyBatis 字段级加密功能配置完成");
            log.info("💡 使用方法：在需要加密的字段上添加 @Encrypted 注解即可");
        }
    }

    /**
     * 加密配置属性类
     */
    @org.springframework.context.annotation.Configuration
    @ConditionalOnProperty(name = "encryption.enabled", havingValue = "true", matchIfMissing = true)
    public static class EncryptionProperties {

        /**
         * 是否启用加密功能
         */
        private boolean enabled = true;

        /**
         * 默认加密算法
         */
        private String algorithm = "AES-GCM";

        /**
         * 密钥
         */
        private String secretKey = "MySecretKey12345MySecretKey12345";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}