package org.example.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/**
 * Этот класс отвечает за чтение файлов миграции и отката.
 * определенных директорий и возвращает их в виде структурированных данных.
 * <p>
 * Он обрабатывает SQL-файлы на предмет миграций и откатов.
 * версии, описания и содержания, а также сортирует их по мере необходимости.
 * </p>
 */

public class MigrationFileReader {
    private final Path migrationDir = Paths.get("src/main/resources/migrations");
    private final Path rollbackDir = Paths.get("src/main/resources/rollbacks");
    private static final Logger logger = LoggerFactory.getLogger(MigrationFileReader.class);


    /**
     * Считывает и обрабатывает все файлы миграции из каталога migrations.
     * <p>
     * Ожидается, что каждый файл будет иметь имя «V<version>__<description>.sql».
     * Файлы сортируются по номеру версии в порядке возрастания.
     * </p>
     *
     * @retur список объектов MigrationFile, содержащих версию, описание,
     * и SQL-содержимое файлов миграции.
     * @throws IOException, если при доступе к файлам возникла ошибка.
     */

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
    /**
     * Считывает и обрабатывает файлы отката из каталога rollbacks, которые соответствуют
     * указанному диапазону версий.
     * <p>
     * Ожидается, что файлы отката будут иметь имя.
     * «V<version>__rollback.sql». Файлы в указанном диапазоне версий
     * отсортированы в порядке убывания.
     * </p>
     *
     * @param targetVersion целевая версия для отката.
     * @param currentVersion текущая версия приложения.
     * @return список объектов {@code MigrationFile}, содержащих версию, описание,
     * и SQL-содержимое файлов отката.
     * @throws IOException, если при доступе к файлам возникла ошибка.
     */
    public List<MigrationFile> getRollbackFiles(String targetVersion, String currentVersion) throws IOException {
        logger.info("Fetching rollback files for target version: " + targetVersion + ", current version: " + currentVersion);
        DirectoryStream<Path> rollbackFiles = Files.newDirectoryStream(rollbackDir, "V*__rollback.sql");
        List<MigrationFile> filteredRollbacks = new ArrayList<>();

        for (Path file : rollbackFiles) {
            String fileName = file.getFileName().toString();
            String version = extractVersion(fileName);

            // Обрабатывает список файлов отката, фильтруя их по целевой и текущей версиям.
            if (version.compareTo(targetVersion) >= 0 && version.compareTo(currentVersion) <= 0) {
                logger.debug("Including rollback file: " + fileName);
                String sqlContent = Files.readString(file);
                filteredRollbacks.add(new MigrationFile(version, "rollback_file", sqlContent));
            } else {
                logger.debug("Excluding rollback file: " + fileName);
            }
        }

        // Сортируйте файлы отката в порядке убывания, чтобы применить их в обратном порядке
        filteredRollbacks.sort(Comparator.comparing(MigrationFile::getVersion).reversed());
        logger.info("Filtered and sorted rollback files: " + filteredRollbacks);
        return filteredRollbacks;
    }


    /**
     * Извлекает версию из имени файла.
     * <p>
     * Версия ожидается как часть имени файла, которая следует за «V»
     * и предшествует «__».
     * </p>
     *
     * @param fileName имя файла.
     * @return извлеченная версия в виде строки.
     */
    public String extractVersion(String fileName) {
        return fileName.split("__")[0].replace("V", "");
    }
    /**
     * Извлекает описание из имени файла.
     * <p>
     * Ожидается, что описание - это часть имени файла, которая следует за
     * «__» и предшествует «.sql».
     * </p>
     *
     * @param fileName имя файла.
     * @return извлеченное описание в виде строки.
     */
    public String extractDescription(String fileName) {
        return fileName.split("__")[1].replace(".sql", "");
    }
}
