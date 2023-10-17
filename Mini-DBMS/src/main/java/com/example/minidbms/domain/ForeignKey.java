package com.example.minidbms.domain;

public class ForeignKey {
    private String fkAttribute;
//    private String foreignAttribute;
    private String refTable;
    private String refAttribute;

    public ForeignKey() {
    }

    //    public ForeignKey(String columnName, String foreignAttribute, String referencedTable, String referencedColumn) {
//        this.columnName = columnName;
//        this.foreignAttribute = foreignAttribute;
//        this.referencedTable = referencedTable;
//        this.referencedColumn = referencedColumn;
//    }

    public ForeignKey(String fkAttribute, String refTable, String refAttribute) {
        this.fkAttribute = fkAttribute;
        this.refTable = refTable;
        this.refAttribute = refAttribute;
    }

    // Getter and Setter methods for the fields

    public String getFkAttribute() {
        return fkAttribute;
    }

    public void setFkAttribute(String fkAttribute) {
        this.fkAttribute = fkAttribute;
    }

//    public String getForeignAttribute() {
//        return foreignAttribute;
//    }
//
//    public void setForeignAttribute(String foreignAttribute) {
//        this.foreignAttribute = foreignAttribute;
//    }

    public String getRefTable() {
        return refTable;
    }

    public void setRefTable(String refTable) {
        this.refTable = refTable;
    }

    public String getRefAttribute() {
        return refAttribute;
    }

    public void setRefAttribute(String refAttribute) {
        this.refAttribute = refAttribute;
    }
}