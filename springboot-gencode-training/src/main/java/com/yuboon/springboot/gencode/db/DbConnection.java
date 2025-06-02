package com.yuboon.springboot.gencode.db;

import com.yuboon.springboot.gencode.exception.GenCodeException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接
 *
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class DbConnection {

    public static Connection  getConnection(){
        Connection conn = null;
        try {
            YamlDbProperties properties = YamlDbProperties.getValue();
            conn = DriverManager.getConnection(properties.getUrl(),properties.getUsername(),properties.getPassword());
        } catch (SQLException e) {
            throw new GenCodeException("数据库连接获取异常",e);
        }

        return conn;
    }

    public static void closeConn(Connection conn){
        try {
            if(conn != null){
                conn.close();
            }
        } catch (SQLException e) {
            throw new GenCodeException("数据库连接关闭异常",e);
        }
    }


}
