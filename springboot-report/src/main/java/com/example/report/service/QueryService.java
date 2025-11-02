package com.example.report.service;

import com.example.report.entity.QueryRequest;
import com.example.report.entity.ColumnInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MetaDataService metaDataService;

    public List<Map<String, Object>> executeQuery(QueryRequest request, String databaseName) {
        String sql = buildSQL(request, databaseName);
        System.out.println("Generated SQL: " + sql);

        if (request.getLimit() > 0) {
            sql += " LIMIT " + request.getLimit();
        }

        return jdbcTemplate.queryForList(sql);
    }

    private String buildSQL(QueryRequest request, String databaseName) {
        StringBuilder sql = new StringBuilder();

        // SELECT clause
        sql.append("SELECT ");

        List<String> selectFields = new ArrayList<>();
        boolean hasDimensions = request.getDimensions() != null && !request.getDimensions().isEmpty();
        boolean hasMetrics = request.getMetrics() != null && !request.getMetrics().isEmpty();

        // Add dimensions
        if (hasDimensions) {
            selectFields.addAll(request.getDimensions());
        }

        // Add metrics with aggregation
        if (hasMetrics) {
            List<ColumnInfo> columns = metaDataService.getTableColumns(databaseName, request.getTableName());

            for (String metric : request.getMetrics()) {
                String dataType = getDataTypeForColumn(columns, metric);
                String aggFunction = getAggregationFunction(dataType);
                selectFields.add(aggFunction + "(" + metric + ") as " + metric);
            }
        } else if (hasDimensions) {
            // 智能优化：如果只有维度没有指标，自动添加 COUNT(*) 作为默认指标
            // 这样前端就能看到每个维度的记录数，而不是重复数据
            selectFields.add("COUNT(*) as record_count");
        }

        // If no dimensions and no metrics, select all
        if (selectFields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectFields));
        }

        // FROM clause
        sql.append(" FROM ").append(databaseName).append(".").append(request.getTableName());

        // WHERE clause
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditions = new ArrayList<>();

            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                String condition = buildCondition(filter);
                if (condition != null) {
                    conditions.add(condition);
                }
            }

            if (!conditions.isEmpty()) {
                sql.append(String.join(" AND ", conditions));
            }
        }

        // GROUP BY clause - 修复：只要有多个维度就应该分组
        // 多个维度时，必须分组以避免数据重复
        if (hasDimensions) {
            // 重要：只要有多个维度就生成 GROUP BY
            sql.append(" GROUP BY ").append(String.join(", ", request.getDimensions()));
        }

        return sql.toString();
    }

    private String buildCondition(QueryRequest.FilterCondition filter) {
        if (filter.getField() == null || filter.getOperator() == null || filter.getValue() == null) {
            return null;
        }

        String field = filter.getField();
        String operator = filter.getOperator();
        String value = filter.getValue();

        switch (operator.toLowerCase()) {
            case "=":
            case "eq":
                return field + " = '" + escapeSql(value) + "'";
            case "!=":
            case "ne":
                return field + " != '" + escapeSql(value) + "'";
            case ">":
            case "gt":
                return field + " > " + value;
            case ">=":
            case "gte":
                return field + " >= " + value;
            case "<":
            case "lt":
                return field + " < " + value;
            case "<=":
            case "lte":
                return field + " <= " + value;
            case "like":
                return field + " LIKE '%" + escapeSql(value) + "%'";
            case "in":
                return field + " IN (" + value + ")";
            default:
                return null;
        }
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private String getDataTypeForColumn(List<ColumnInfo> columns, String columnName) {
        for (ColumnInfo column : columns) {
            if (column.getColumnName().equals(columnName)) {
                return column.getDataType();
            }
        }
        return "varchar";
    }

    private String getAggregationFunction(String dataType) {
        if (dataType != null && (dataType.toLowerCase().contains("int") ||
            dataType.toLowerCase().contains("decimal") ||
            dataType.toLowerCase().contains("float") ||
            dataType.toLowerCase().contains("double"))) {
            return "SUM";
        }
        return "COUNT";
    }
}
