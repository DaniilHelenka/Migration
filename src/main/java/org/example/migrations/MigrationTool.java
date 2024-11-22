package org.example.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class MigrationTool {
    private final MigrationExecutor migrationExecutor;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(MigrationTool.class);
    private final MigrationFileReader migrationFileReader = new MigrationFileReader();

    public MigrationTool(MigrationExecutor migrationExecutor, Connection connection) {
        this.migrationExecutor = migrationExecutor;
        this.connection = connection;
    }

    public void executeMigration() throws SQLException {
        migrationExecutor.initializeSchemaTable();
        logger.info("Migration starts");

        try {
            connection.setAutoCommit(false);

            String currentVersion = migrationExecutor.getCurrentVersion();
            logger.info("Current database version: " + currentVersion);

            List<MigrationFile> migrationFiles = migrationFileReader.getMigrationFiles();
            for (MigrationFile migrationFile : migrationFiles) {
                if (currentVersion == null || migrationFile.getVersion().compareTo(currentVersion) > 0) {
                    migrationExecutor.applyMigration(
                            migrationFile.getVersion(),
                            migrationFile.getDescription(),
                            migrationFile.getSql(),
                            "V" + migrationFile.getVersion() + "__rollback.sql"
                    );
                }
            }
            connection.commit();
            logger.info("All migrations applied successfully");


        } catch (SQLException | IOException e) {

            connection.rollback();
            logger.info("Migration process failed: {}", e.getMessage(), e);
            throw new SQLException("Migration process failed", e);
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void executeRollback() throws SQLException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter operation: [migrate/rollback]");
        String targetVersion  = scanner.nextLine();
        logger.info("Rollback starts for target version: " + targetVersion);

        try {
            connection.setAutoCommit(false);

            String currentVersion = migrationExecutor.getCurrentVersion();
            if (currentVersion == null || targetVersion.compareTo(currentVersion) >= 0) {
                logger.info("No rollback needed. Target version: " + targetVersion + ", Current version: " + currentVersion);
                return;
            }

            logger.info("Current database version: " + currentVersion);

            // Fetch rollback files for the range (targetVersion, currentVersion]
            List<MigrationFile> rollbackFiles = migrationFileReader.getRollbackFiles(targetVersion, currentVersion);
            for (MigrationFile rollbackFile : rollbackFiles) {
                logger.info("Executing rollback for version: " + rollbackFile.getVersion());
                logger.info("SQL to execute: " + rollbackFile.getSql());
                migrationExecutor.rollbackMigration(
                        rollbackFile.getDescription(),
                        rollbackFile.getSql(),
                        rollbackFile.getVersion()
                );
                System.out.println(rollbackFile.getDescription() + rollbackFile.getSql() + rollbackFile.getVersion());
            }

            // Remove applied_migration entries for versions higher than the target version
            String deleteVersionsSql = "DELETE FROM applied_migration WHERE version > ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteVersionsSql)) {
                preparedStatement.setString(1, targetVersion);
                preparedStatement.executeUpdate();
            }

            connection.commit();
            logger.info("Rollback completed successfully to version: " + targetVersion);
        } catch (SQLException | IOException e) {
            connection.rollback();
            logger.error("Rollback process failed: {}", e.getMessage(), e);
            throw new SQLException("Rollback process failed", e);
        } finally {
            connection.setAutoCommit(true);
        }
        logger.debug("Migration process ends");
    }
}


