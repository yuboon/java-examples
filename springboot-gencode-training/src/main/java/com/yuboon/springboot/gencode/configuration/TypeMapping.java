package com.yuboon.springboot.gencode.configuration;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;

/**
 * 数据库&Java类型映射
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class TypeMapping {

    private Map<String,String> sqlJavaTypeMapping = new LinkedCaseInsensitiveMap<>();

    public TypeMapping(){
        sqlJavaTypeMapping.put("BIGINT UNSIGNED","lang");
        sqlJavaTypeMapping.put("DATETIME","Date");
        sqlJavaTypeMapping.put("TIMESTAMP","Date");
        sqlJavaTypeMapping.put("VARCHAR","String");
        sqlJavaTypeMapping.put("DECIMAL","BigDecimal");
        sqlJavaTypeMapping.put("BIGINT","long");
        sqlJavaTypeMapping.put("TEXT","String");
        sqlJavaTypeMapping.put("TINYINT","int");
        sqlJavaTypeMapping.put("INT","int");
        sqlJavaTypeMapping.put("BIT","int");
        sqlJavaTypeMapping.put("CHAR","String");
    }

    public void addMapping(String dbType,String javaType){
        sqlJavaTypeMapping.put(dbType,javaType);
    }

    public String getJavaType(String dbType){
        return sqlJavaTypeMapping.get(dbType);
    }

    /**
     * 覆盖默认type映射
     * @param dbType
     * @param javaType
     */
    public void overriedTypeMapping(String dbType,String javaType){
        sqlJavaTypeMapping.replace(dbType,javaType);
    }

}
