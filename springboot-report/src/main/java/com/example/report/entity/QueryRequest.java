package com.example.report.entity;

import java.util.List;

public class QueryRequest {
    private String tableName;
    private List<String> dimensions;
    private List<String> metrics;
    private List<FilterCondition> filters;
    private int limit = 100;

    public static class FilterCondition {
        private String field;
        private String operator;
        private String value;

        public FilterCondition() {}

        public FilterCondition(String field, String operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public QueryRequest() {}

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public List<String> getDimensions() { return dimensions; }
    public void setDimensions(List<String> dimensions) { this.dimensions = dimensions; }
    public List<String> getMetrics() { return metrics; }
    public void setMetrics(List<String> metrics) { this.metrics = metrics; }
    public List<FilterCondition> getFilters() { return filters; }
    public void setFilters(List<FilterCondition> filters) { this.filters = filters; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}