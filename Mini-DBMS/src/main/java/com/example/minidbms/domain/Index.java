package com.example.minidbms.domain;

import java.util.List;

public class Index {
    private String indexName;
    private String tableName;
    private List<String> columns;

    public Index(String indexName, String tableName, List<String> columns) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columns = columns;
    }

    // Getter and Setter methods for the fields

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
