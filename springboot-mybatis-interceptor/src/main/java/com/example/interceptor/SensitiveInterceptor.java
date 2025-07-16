package com.example.interceptor;

import com.example.util.SensitiveUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

@Slf4j
@Intercepts({
    @Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
    )
})
public class SensitiveInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 执行原方法，获取查询结果
        Object result = invocation.proceed();
        
        // 2. 如果结果是List，遍历处理每个元素
        if (result instanceof List<?>) {
            List<?> resultList = (List<?>) result;
            for (Object obj : resultList) {
                // 3. 对有@Sensitive注解的字段进行脱敏
                desensitize(obj);
            }
        }
        return result;
    }

    // 反射处理对象中的敏感字段
    private void desensitize(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return;
        }
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();  // 获取所有字段（包括私有）
        
        for (Field field : fields) {
            // 4. 检查字段是否有@Sensitive注解
            if (field.isAnnotationPresent(Sensitive.class)) {
                Sensitive annotation = field.getAnnotation(Sensitive.class);
                field.setAccessible(true);  // 开启私有字段访问权限
                Object value = field.get(obj);  // 获取字段值
                
                if (value instanceof String) {
                    String strValue = (String) value;
                    // 5. 根据脱敏类型处理
                    switch (annotation.type()) {
                        case PHONE:
                            field.set(obj, SensitiveUtils.maskPhone(strValue));
                            break;
                        case ID_CARD:
                            field.set(obj, SensitiveUtils.maskIdCard(strValue));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可配置更多脱敏规则，此处省略
    }
}