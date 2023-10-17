package com.example.minidbms.domain;

public class PrimaryKey {
    private String pkAttribute;

    public PrimaryKey() {
    }

    public PrimaryKey(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }

    // Getter and Setter methods for the field

    public String getPkAttribute() {
        return pkAttribute;
    }

    public void setPkAttribute(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }
}

