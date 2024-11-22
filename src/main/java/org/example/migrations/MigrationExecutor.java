package org.example.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;


public class MigrationExecutor {
    private final Connection connection;
    MigrationFileReader fileReader;
    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

    public MigrationExecutor(Connection connection, MigrationFileReader fileReader) {
        this.connection = connection;
        this.fileReader = fileReader;
    }

    public void initializeSchemaTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS applied_migration (
                    version VARCHAR(50) PRIMARY KEY,
                    description VARCHAR(255),
                    applied_at TIMESTAMP,
                    rollback_file VARCHAR(255)
                );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public List<String> getAppliedMigrations() throws SQLException {
        String sql = "SELECT version FROM applied_migration ORDER BY version";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<String> appliedMigrations = new java.util.ArrayList<>();
            while (resultSet.next()) {
                appliedMigrations.add(resultSet.getString("version"));
            }
            return appliedMigrations;
        }
    }
    public String getCurrentVersion() throws SQLException{
        String sql = "SELECT version FROM applied_migration ORDER BY version DESC LIMIT 1";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                return resultSet.getString("version");
            }
            return null;
        }
    }

    public void applyMigration(String version, String description, String sql, String rollback_file) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }

            String insertVersionSql = "INSERT INTO applied_migration (version, description, applied_at, rollback_file) VALUES (?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertVersionSql)) {
                preparedStatement.setInt(1, Integer.parseInt(version));
                preparedStatement.setString(2, description);
                preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.setString(4, rollback_file);
                preparedStatement.executeUpdate();
            }
    }
    public void rollbackMigration( String version, String description, String sql) throws SQLException {
        logger.info("Rolling back version: " + sql);
        logger.info("SQL to execute: " + sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(description);
        }
        logger.info("Rollback SQL applied for version: " + sql);
    }
}


