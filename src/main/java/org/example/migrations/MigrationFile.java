package org.example.migrations;

public class MigrationFile {
    private final String version;
    private final String description;
    private final String sql;

    public MigrationFile(String version, String description, String sql) {
        this.version = version;
        this.description = description;
        this.sql = sql;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getSql() {
        return sql;
    }
}
