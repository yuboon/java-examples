package com.example.timingwheel.config;

import com.example.timingwheel.model.TimingWheelProperties;
import com.example.timingwheel.util.TimingWheel;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 时间轮配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TimingWheelProperties.class)
public class TimingWheelConfig {

    @Bean
    public TimingWheel timingWheel(TimingWheelProperties properties, MeterRegistry meterRegistry) {
        log.info("Creating timing wheel with properties: {}", properties);
        return new TimingWheel(properties, meterRegistry);
    }
}