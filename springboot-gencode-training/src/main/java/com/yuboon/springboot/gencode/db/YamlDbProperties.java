package com.yuboon.springboot.gencode.db;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * yaml配置文件数据库连接属性获取
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class YamlDbProperties {

    private String url;

    private String username;

    private String password;

    // 获取配置文件的值
    public static YamlDbProperties getValue(){
        YamlDbProperties yamlDbProperties = new YamlDbProperties();

        Yaml yaml = new Yaml();
        Map<String, Object> ret = (Map<String, Object>) yaml.load(YamlDbProperties.class.getClassLoader().getResourceAsStream("application.yaml"));
        Map<String,Object> spring = (Map<String,Object> )ret.get("spring");
        Map<String,Object> datasource = (Map<String,Object> )spring.get("datasource");

        String url = String.valueOf(datasource.get("url"));
        String username = String.valueOf(datasource.get("username"));
        String password = String.valueOf(datasource.get("password"));

        yamlDbProperties.setUrl(url);
        yamlDbProperties.setUsername(username);
        yamlDbProperties.setPassword(password);
        return yamlDbProperties;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
