package com.example.minidbms.domain;

public class PrimaryKey {
    private String columnName;

    public PrimaryKey() {
    }

    public PrimaryKey(String columnName) {
        this.columnName = columnName;
    }

    // Getter and Setter methods for the field

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}

