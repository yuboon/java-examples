package com.example.report.service;

import com.example.report.entity.TableInfo;
import com.example.report.entity.ColumnInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MetaDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<TableInfo> getDatabaseTables(String databaseName) {
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT, " +
                "(SELECT COUNT(*) FROM information_schema.COLUMNS C WHERE C.TABLE_NAME = T.TABLE_NAME AND C.TABLE_SCHEMA = T.TABLE_SCHEMA) as COLUMN_COUNT " +
                "FROM information_schema.TABLES T " +
                "WHERE T.TABLE_SCHEMA = ? AND T.TABLE_TYPE = 'BASE TABLE' " +
                "ORDER BY TABLE_NAME";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, databaseName);
        List<TableInfo> tables = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            TableInfo table = new TableInfo();
            table.setTableName((String) row.get("TABLE_NAME"));
            table.setTableComment((String) row.get("TABLE_COMMENT"));
            table.setColumnCount(((Number) row.get("COLUMN_COUNT")).intValue());
            tables.add(table);
        }

        return tables;
    }

    public List<ColumnInfo> getTableColumns(String databaseName, String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_TYPE " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, databaseName, tableName);
        List<ColumnInfo> columns = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            ColumnInfo column = new ColumnInfo();
            column.setColumnName((String) row.get("COLUMN_NAME"));
            column.setDataType((String) row.get("DATA_TYPE"));
            column.setColumnComment((String) row.get("COLUMN_COMMENT"));
            column.setNullable("YES".equals(row.get("IS_NULLABLE")));
            column.setColumnType((String) row.get("COLUMN_TYPE"));
            columns.add(column);
        }

        return columns;
    }

    public List<String> getDatabases() {
        String sql = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA " +
                "WHERE SCHEMA_NAME NOT IN ('information_schema', 'performance_schema', 'mysql', 'sys') " +
                "ORDER BY SCHEMA_NAME";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<String> databases = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            databases.add((String) row.get("SCHEMA_NAME"));
        }

        return databases;
    }
}