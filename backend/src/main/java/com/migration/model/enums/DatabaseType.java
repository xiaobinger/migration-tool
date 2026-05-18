package com.migration.model.enums;

import lombok.Getter;

@Getter
public enum DatabaseType {
    MYSQL("mysql", "MySQL"),
    POSTGRESQL("postgresql", "PostgreSQL"),
    MONGODB("mongodb", "MongoDB");

    private final String code;
    private final String label;

    DatabaseType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DatabaseType fromCode(String code) {
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) return type;
        }
        throw new IllegalArgumentException("Unknown DatabaseType: " + code);
    }
}
