package com.example.config;

import com.example.interceptor.SensitiveInterceptor;
import com.example.interceptor.SlowSqlInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@MapperScan("com.example.mapper")  // Mapper接口所在包
public class MyBatisConfig {

    // 注册慢查询拦截器
    @Bean
    public SlowSqlInterceptor slowSqlInterceptor() {
        SlowSqlInterceptor interceptor = new SlowSqlInterceptor();
        
        // 设置属性（也可通过application.yml配置）
        Properties properties = new Properties();
        properties.setProperty("slowThreshold", "500");  // 慢查询阈值500ms
        interceptor.setProperties(properties);
        
        return interceptor;
    }

    @Bean
    public SensitiveInterceptor sensitiveInterceptor() {
        return new SensitiveInterceptor();
    }

    // 将拦截器添加到SqlSessionFactory
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, SlowSqlInterceptor slowSqlInterceptor, SensitiveInterceptor sensitiveInterceptor) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // 设置Mapper.xml路径（如果需要）
        /*sessionFactory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml")
        );*/
        
        // 添加拦截器
        sessionFactory.setPlugins(slowSqlInterceptor,sensitiveInterceptor);

        return sessionFactory.getObject();
    }
}