package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public class ConfigRefreshHandler implements ApplicationContextAware {
    @Autowired
    private ConfigurableEnvironment environment;
    private ApplicationContext applicationContext;

    @Autowired
    private ConfigurationPropertiesBindingPostProcessor bindingPostProcessor; // 属性绑定工具

    // 刷新配置的核心方法
    public void refresh() {
        try {
            // 1. 重新读取配置文件内容
            Properties properties = loadConfigFile();

            // 2. 更新Environment中的属性
            Set<String> changeKeys = updateEnvironment(properties);

            // 3. 重新绑定所有@ConfigurationProperties Bean
            if (!changeKeys.isEmpty()) {
                rebindConfigurationProperties();
            }

            applicationContext.publishEvent( new ConfigRefreshedEvent(this,changeKeys));
            log.info("配置文件刷新完成");
        } catch (Exception e) {
            log.error("配置文件刷新失败", e);
        }
    }

    // 读取配置文件内容（支持properties和yaml）
    private Properties loadConfigFile() throws IOException {
        // 使用Spring工具类读取classpath下的配置文件
        Resource resource = new ClassPathResource("application.yaml");
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(resource);

        // 获取解析后的Properties对象
        Properties properties = yamlFactory.getObject();
        if (properties == null) {
            throw new IOException("Failed to load configuration file");
        }
        return properties;
    }

    // 更新Environment中的属性，返回变化的配置键集合
    private Set<String> updateEnvironment(Properties properties) {
        String sourceName = "Config resource 'class path resource [application.yaml]' via location 'optional:classpath:/'";
        Set<String> changedKeys = new HashSet<>();
        PropertySource<?> appConfig = environment.getPropertySources().get(sourceName);

        if (appConfig instanceof MapPropertySource) {
            Map<String, Object> sourceMap = new HashMap<>(((MapPropertySource) appConfig).getSource());

            properties.forEach((k, v) -> {
                String key = k.toString();
                Object oldValue = sourceMap.get(key);
                if (!Objects.equals(oldValue, v)) {
                    changedKeys.add(key);
                }
                sourceMap.put(key, v);
            });

            environment.getPropertySources().replace(sourceName, new MapPropertySource(sourceName, sourceMap));
        }
        return changedKeys;
    }

    // 重新绑定所有@ConfigurationProperties Bean
    private void rebindConfigurationProperties() {
        // 获取所有@ConfigurationProperties Bean的名称
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
        for (String beanName : beanNames) {
            // 重新绑定属性（关键：不重建Bean，只更新属性值）
            bindingPostProcessor.postProcessBeforeInitialization(
                    applicationContext.getBean(beanName), beanName);
            log.info("刷新配置Bean：{}", beanName);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}