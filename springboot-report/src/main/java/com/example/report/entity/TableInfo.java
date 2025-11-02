package com.example.report.entity;

public class TableInfo {
    private String tableName;
    private String tableComment;
    private int columnCount;

    public TableInfo() {}

    public TableInfo(String tableName, String tableComment, int columnCount) {
        this.tableName = tableName;
        this.tableComment = tableComment;
        this.columnCount = columnCount;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }
}