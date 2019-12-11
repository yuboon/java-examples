package com.yuboon.learning.exception;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    Demo demo (){
        String nullStr = null;
        //nullStr.toString();
        return new Demo();
    }

}