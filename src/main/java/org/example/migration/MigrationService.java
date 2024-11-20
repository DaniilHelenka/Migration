package org.example.migration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class MigrationService {
    private final Connection connection;

    public MigrationService(Connection connection) {
        this.connection = connection;
    }


    public void initializeSchemaTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS applied_migrations (
                    version VARCHAR(50) PRIMARY KEY,
                    description VARCHAR(255),
                    applied_at TIMESTAMP
                );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public List<String> getAppliedMigrations() throws SQLException {
        String sql = "SELECT version FROM applied_migrations";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<String> appliedMigrations = new java.util.ArrayList<>();
            while (resultSet.next()) {
                appliedMigrations.add(resultSet.getString("version"));
            }
            return appliedMigrations;
        }
    }

    public void applyMigration(String version, String description, String sql) throws SQLException {
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            String insertVersionSql = "INSERT INTO applied_migrations (version, description, applied_at) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertVersionSql)) {
                preparedStatement.setString(1, version);
                preparedStatement.setString(2, description);
                preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.executeUpdate();
            }
            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            System.err.println("Error applying migration: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public int getCurrentVersion() throws SQLException {
        String query = "SELECT MAX(version) FROM applied_migrations";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);  // Возвращаем максимальную версию
            } else {
                return 0;  // Если миграций нет, версия = 0
            }
        }
    }

}