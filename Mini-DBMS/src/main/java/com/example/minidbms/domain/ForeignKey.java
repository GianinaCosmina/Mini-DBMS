package com.example.minidbms.domain;

public class ForeignKey {
    private String columnName;
    private String foreignAttribute;
    private String referencedTable;
    private String referencedColumn;

    public ForeignKey(String columnName, String foreignAttribute, String referencedTable, String referencedColumn) {
        this.columnName = columnName;
        this.foreignAttribute = foreignAttribute;
        this.referencedTable = referencedTable;
        this.referencedColumn = referencedColumn;
    }

    // Getter and Setter methods for the fields

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getForeignAttribute() {
        return foreignAttribute;
    }

    public void setForeignAttribute(String foreignAttribute) {
        this.foreignAttribute = foreignAttribute;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    public String getReferencedColumn() {
        return referencedColumn;
    }

    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }
}