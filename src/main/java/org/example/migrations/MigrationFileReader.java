package org.example.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MigrationFileReader {
    private final Path migrationDir = Paths.get("src/main/resources/migrations");
    private final Path rollbackDir = Paths.get("src/main/resources/rollbacks");
    private static final Logger logger = LoggerFactory.getLogger(MigrationFileReader.class);


    public MigrationFileReader() {

    }

    public List<MigrationFile> getMigrationFiles() throws IOException {
        DirectoryStream<Path> migrationFiles = Files.newDirectoryStream(migrationDir, "*.sql");
        List<MigrationFile> sortedMigrations = new ArrayList<>();

        for (Path file : migrationFiles) {
            String fileName = file.getFileName().toString();
            String version = extractVersion(fileName);
            String description = extractDescription(fileName);
            String sqlContent = Files.readString(file);

            sortedMigrations.add(new MigrationFile(version, description, sqlContent));
        }

        sortedMigrations.sort(Comparator.comparing(MigrationFile::getVersion));
        return sortedMigrations;
    }

    public List<MigrationFile> getRollbackFiles(String targetVersion, String currentVersion) throws IOException {
        logger.info("Fetching rollback files for target version: " + targetVersion + ", current version: " + currentVersion);
        DirectoryStream<Path> rollbackFiles = Files.newDirectoryStream(rollbackDir, "V*__rollback.sql");
        List<MigrationFile> filteredRollbacks = new ArrayList<>();

        for (Path file : rollbackFiles) {
            String fileName = file.getFileName().toString();
            String version = extractVersion(fileName);

            // Include files with version greater than or equal to targetVersion and less than or equal to currentVersion
            if (version.compareTo(targetVersion) >= 0 && version.compareTo(currentVersion) <= 0) {
                logger.debug("Including rollback file: " + fileName);
                String sqlContent = Files.readString(file);
                filteredRollbacks.add(new MigrationFile(version, "rollback_file", sqlContent));
            } else {
                logger.debug("Excluding rollback file: " + fileName);
            }
        }

        // Sort rollback files in descending order to apply them in reverse order
        filteredRollbacks.sort(Comparator.comparing(MigrationFile::getVersion).reversed());
        logger.info("Filtered and sorted rollback files: " + filteredRollbacks);
        return filteredRollbacks;
    }



    private String extractVersion(String fileName) {
        return fileName.split("__")[0].replace("V", "");
    }

    private String extractDescription(String fileName) {
        return fileName.split("__")[1].replace(".sql", "");
    }
}
