package org.example.migration;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MigrateCommand {
    private final MigrationService migrationService;


    public MigrateCommand(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public void execute() throws SQLException, IOException {
        migrationService.initializeSchemaTable();

        List<String> appliedMigrations = migrationService.getAppliedMigrations();

        Path migrationDir = Paths.get("src/main/resources/migrations");
        DirectoryStream<Path> migrationFiles = Files.newDirectoryStream(migrationDir, "*.sql");


        for (Path file : migrationFiles) {
            String fileName = file.getFileName().toString();
            String version = fileName.split("__")[0].replace("V", "");
            String description = fileName.split("__")[1].replace(".sql", "");
            int currentVersion = migrationService.getCurrentVersion();
            int migrationVersion = Integer.parseInt(version);
            if (migrationVersion > currentVersion && !appliedMigrations.contains(version)) {
                String sql = Files.readString(file);
                migrationService.applyMigration(version, description, sql);
                System.out.println("Applied migration: " + fileName);
            }
        }
    }
}