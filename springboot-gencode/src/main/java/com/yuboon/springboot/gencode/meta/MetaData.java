package com.yuboon.springboot.gencode.meta;

import cn.hutool.core.collection.CollectionUtil;
import com.yuboon.springboot.gencode.exception.GenCodeException;
import com.yuboon.springboot.gencode.configuration.TypeMapping;
import com.yuboon.springboot.gencode.db.DbConnection;

import java.sql.*;
import java.util.List;

/**
 * 元数据操作类
 *
 * @author yuboon
 * @version v1.0
 * @date 2020/01/04
 */
public class MetaData {

    /**
     * 生成代码
     * @return
     */
    public static Table getTableInfo(String tableName, TypeMapping typeMapping){
        String result = "";
        Connection conn = DbConnection.getConnection();
        Table table = new Table();
        try {
            table.setCode(tableName);

            // 表元数据
            Statement stmt = conn.createStatement();
            String sql = "select table_comment from information_schema.tables where table_name = '"+ tableName + "'";
            ResultSet tableResultSet = stmt.executeQuery(sql);
            while (tableResultSet.next()) {
                table.setComment(tableResultSet.getString("table_comment"));
                break;
            }

            // 字段元数据
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, table.getCode(), "%");
            List<TableColumn> tableColumnList = CollectionUtil.newArrayList();
            while (resultSet.next()) {
                TableColumn tableColumn = new TableColumn();
                tableColumn.setCode(resultSet.getString("column_name").toLowerCase());
                tableColumn.setType(typeMapping.getJavaType(resultSet.getString("type_name")));
                tableColumn.setComment(resultSet.getString("remarks"));
                tableColumnList.add(tableColumn);
            }
            table.setColumnList(tableColumnList);

            tableResultSet.close();
            stmt.close();
            resultSet.close();
        } catch (SQLException e) {
            throw new GenCodeException("获取表元数据信息出错",e);
        } finally {
            // 关闭连接
            DbConnection.closeConn(conn);
        }
        return table;
    }

}
