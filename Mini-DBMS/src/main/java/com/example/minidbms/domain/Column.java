package com.example.minidbms.domain;


import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "column")
@XmlType(propOrder = { "columnName", "type" })
public
class Column {
    @XmlAttribute
    private String columnName;
    @XmlAttribute
    private String type;

    public Column() {
    }

    public Column(String name, String type) {
        this.columnName = name;
        this.type = type;
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
}
