package com.example.minidbms.domain;


import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "column")
@XmlType(propOrder = { "columnName", "type", "isPrimaryKey" })
class Column {
    @XmlAttribute
    private String columnName;
    @XmlAttribute
    private String type;
    private boolean isPrimaryKey;

    public Column() {
    }

    public Column(String name, String type, boolean isPrimaryKey) {
        this.columnName = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }
}
