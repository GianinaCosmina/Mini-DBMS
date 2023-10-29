package com.example.minidbms.domain;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "tables")
@XmlType(name = "tables", propOrder = { "tableName", "fileName", "columns", "primaryKeys", "foreignKeys", "indexes" })
public
class Table {
    @XmlAttribute
    private String tableName;
    @XmlAttribute
    private String fileName;
    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "structure")
    private List<Column> columns;
    @XmlElement(name = "pkAttribute")
    @XmlElementWrapper(name = "primaryKey")
    private List<PrimaryKey> primaryKeys;
    @XmlElement(name = "foreignKey")
    @XmlElementWrapper(name = "foreignKeys")
    private List<ForeignKey> foreignKeys;
    @XmlElement(name = "IndexAttribute")
    @XmlElementWrapper(name = "IndexAttributes")
    private List<Index> indexes;

    public Table() {
    }

    public Table(String name) {
        this.tableName = name;
        this.columns = new ArrayList<>();
        this.primaryKeys = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.indexes = new ArrayList<>();
    }

    public Table(String tableName, List<Column> columns, List<PrimaryKey> primaryKeys, List<ForeignKey> foreignKeys) {
        this.tableName = tableName;
        this.fileName =  tableName + ".bin" ;
        this.columns = columns;
        this.primaryKeys = primaryKeys;
        this.foreignKeys = foreignKeys;
        this.indexes = new ArrayList<>();
    }

        public void createIndex(Index newIndex) {
        indexes.add(newIndex);

    }

    public void dropIndex(String indexName) {
        Index indexToRemove = null;
        for (Index index : indexes) {
            if (index.getIndexName().equalsIgnoreCase(indexName)) {
                indexToRemove = index;
                break;
            }
        }
        if (indexToRemove != null) {
            indexes.remove(indexToRemove);
        }
    }

    public Column getColumnByName(String name) {
        for (Column column: this.columns) {
            if (column.getColumnName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<PrimaryKey> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<PrimaryKey> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<ForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }
}