package com.example.report.entity;

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private String columnComment;
    private boolean isNullable;
    private String columnType;

    public ColumnInfo() {}

    public ColumnInfo(String columnName, String dataType, String columnComment, boolean isNullable, String columnType) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.columnComment = columnComment;
        this.isNullable = isNullable;
        this.columnType = columnType;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }
}