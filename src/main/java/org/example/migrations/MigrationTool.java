package org.example.migrations;

import org.example.migrations.fileReader.MigrationFile;
import org.example.migrations.fileReader.MigrationFileReader;
import org.example.migrations.report.MigrationReport;
import org.example.migrations.report.MigrationReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Этот класс предоставляет функциональность для управления миграциями
 * и откатами баз данных и генерации отчётов в виде файлов.
 * <p>
 * {@code MigrationTool} управляет выполнением SQL-скриптов миграции и сценариев отката.
 * на основе текущей версии схемы базы данных. Он использует MigrationExecutor для
 * применения миграций и управления версионированием схемы.
 * </p>
 */
public class MigrationTool {
    private final MigrationExecutor migrationExecutor;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(MigrationTool.class);
    private final MigrationFileReader migrationFileReader = new MigrationFileReader();
    List<MigrationReport> reports = new ArrayList<>();

    /**
     * Конструирует инструмент MigrationTool с указанным исполнителем MigrationExecutor и подключением к базе данных.
     *
     * @param migrationExecutor исполнитель, отвечающий за применение миграций и управление версионированием схем
     * @param connection        соединение с базой данных, используемое для выполнения SQL-команд
     */
    public MigrationTool(MigrationExecutor migrationExecutor, Connection connection) {
        this.migrationExecutor = migrationExecutor;
        this.connection = connection;
    }

    /**
     * Выполняет все ожидающие миграции, чтобы привести схему базы данных в актуальное состояние.
     * <p>
     * Этот метод считывает файлы миграций, сравнивает их версии с текущей версией базы данных,
     * и применяет только необходимые миграции. Механизм блокировки гарантирует, что только один процесс может
     * выполнять миграци. При возникновении ошибки процесс миграции откатывается, чтобы сохранить
     *  целостности базы данных. Блокировка снимается по завершении процесса, независимо от успеха или неудачи.
     *  Если возникает ошибка, процесс миграции откатывается.
     * </p>
     *
     * @throws SQLException, если во время миграции или отката произошла ошибка базы данных
     */
    public void executeMigration() throws SQLException, IOException {

        migrationExecutor.initializeSchemaTable();
        migrationExecutor.initializeMigrationLockTable();
        String lockedBy = "user-" + System.getProperty("user.name") + "-"
                          + System.currentTimeMillis();

        if (migrationExecutor.isLocked()) {
            logger.error("Migration is already locked by another process.");
            throw new IllegalStateException("Migration is locked. Another process is currently performing a migration.");
        }
        migrationExecutor.lockMigration(lockedBy);
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
                    reports.add(new MigrationReport(
                            migrationFile.getVersion(),
                            migrationFile.getDescription(),
                            true,
                            LocalDateTime.now().toString(),
                            null
                    ));
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
            migrationExecutor.unlockMigration();
        }
        MigrationReportGenerator reportGenerator = new MigrationReportGenerator();
        reportGenerator.generateCsvReport(reports, "src/main/resources/reports/migration_report.csv");
        reportGenerator.generateJsonReport(reports, "src/main/resources/reports/migration_report.json");
    }

    /**
     * Откатывает схему базы данных к определенной целевой версии.
     * <p>
     * Этот метод считывает файлы отката, определяет сценарии, которые необходимо выполнить
     * для отката к целевой версии, и применяет их в обратном порядке.
     * Все примененные миграции с версиями выше, чем
     * целевой версии, удаляются из таблицы отслеживания.
     * </p>
     *
     * @throws SQLException, если во время отката или очистки произошла ошибка базы данных
     */
    public void executeRollback() throws SQLException, IOException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Select the rollback version: ");
        String targetVersion = scanner.nextLine();
        logger.info("Rollback starts for target version: " + targetVersion);

        try {
            connection.setAutoCommit(false);

            String currentVersion = migrationExecutor.getCurrentVersion();
            if (currentVersion == null || targetVersion.compareTo(currentVersion) >= 0) {
                logger.info("No rollback needed. Target version: " + targetVersion + ", Current version: " + currentVersion);
                return;
            }

            logger.info("Current database version: " + currentVersion);

            // Получаем файлы отката для диапазона (targetVersion, currentVersion)
            List<MigrationFile> rollbackFiles = migrationFileReader.getRollbackFiles(targetVersion, currentVersion);
            for (MigrationFile rollbackFile : rollbackFiles) {
                logger.info("Executing rollback for version: " + rollbackFile.getVersion());
                logger.info("SQL to execute: " + rollbackFile.getSql());
                migrationExecutor.rollbackMigration(
                        rollbackFile.getDescription(),
                        rollbackFile.getSql(),
                        rollbackFile.getVersion()
                );
                reports.add(new MigrationReport(
                        rollbackFile.getVersion(),
                        rollbackFile.getDescription(),
                        true,
                        LocalDateTime.now().toString(),
                        null

                ));

                System.out.println(rollbackFile.getDescription() + rollbackFile.getSql() + rollbackFile.getVersion());
            }

            // Удалите записи applied_migration для версий выше целевой версии
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
        MigrationReportGenerator reportGenerator = new MigrationReportGenerator();
        reportGenerator.generateCsvReport(reports, "src/main/resources/reports/migration_report.csv");
        reportGenerator.generateJsonReport(reports, "src/main/resources/reports/migration_report.json");
        logger.debug("Migration process ends");
    }
}


