package org.example.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
/**
 * Управляет выполнением миграций и откатов баз данных.
 * <p>
 * Класс MigrationExecutor предоставляет методы для инициализации таблицы отслеживания схемы,
 * получения текущей версии схемы, применения миграций и отката изменений.
 * </p>
 */
public class MigrationExecutor {
    private final Connection connection;
    MigrationFileReader fileReader;
    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);
    /**
     * Конструирует MigrationExecutor с указанным подключением к базе данных и устройством чтения файлов.
     *
     * @param connection соединение с базой данных для выполнения SQL-команд
     * @param fileReader устройство чтения файлов для чтения файлов миграции и отката
     */
    public MigrationExecutor(Connection connection, MigrationFileReader fileReader) {
        this.connection = connection;
        this.fileReader = fileReader;
    }
    /**
     * Инициализирует таблицу отслеживания схемы, если она еще не существует.
     * <p>
     * В этой таблице хранится информация о примененных миграциях, включая версию, описание, временную метку приложения,
     * и файл отката, связанный с каждой миграцией.
     * </p>
     *
     * @throws SQLException, если при создании таблицы возникла ошибка базы данных.
     */
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
    //-------------------------------------
    public void initializeMigrationLockTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS migration_lock (
                      id INT PRIMARY KEY,
                      locked BOOLEAN NOT NULL,
                      locked_at TIMESTAMP,
                      locked_by VARCHAR(255)
                  );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }


    /**
     * Получает список версий всех примененных миграций.
     *
     * @return список версий миграций, отсортированных в порядке возрастания.
     * @throws SQLException, если при получении версий произошла ошибка базы данных
     */
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
    /**
     * Получает текущую версию схемы базы данных.
     *
     * @ возвращает текущую версию схемы или null, если миграции не применялись.
     * @throws SQLException, если при получении версии произошла ошибка базы данных
     */
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
    /**
     * Проверяет, заблокирован ли в данный момент процесс миграции, запрашивая таблицу `migration_lock`.
     *
     * <p>Этот метод извлекает статус ``заблокирован'' из таблицы. Если запись о блокировке не существует,
     * он предполагает, что блокировки нет, и возвращает false.</p>
     *
     * @return true, если процесс миграции заблокирован;  false в противном случае.
     * @throws SQLException, если при взаимодействии с базой данных произошла ошибка.
     */

    public boolean isLocked() throws SQLException {
        String sql = "SELECT locked FROM migration_lock WHERE id = 1";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getBoolean("locked");
            }
            return false; // Если записи нет, считаем, что блокировки нет
        }
    }
    /**
     * Блокирует процесс миграции, устанавливая статус `locked` в таблице `migration_lock` на `TRUE`.
     * Если запись о блокировке существует, то обновляется информация о блокировке. В противном случае создается новая запись о блокировке.
     *
     * <p>Этот метод гарантирует, что никакие другие процессы не смогут начать миграцию, пока блокировка не будет снята.</p>
     *
     * @param lockedBy - строка, представляющая идентификатор процесса или пользователя, инициировавшего блокировку.
     * Это значение сохраняется в столбце `locked_by` для целей отслеживания.
     * @throws SQLException, если при взаимодействии с базой данных произошла ошибка.
     */
    public void lockMigration(String lockedBy) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM migration_lock WHERE id = 1";
        String insertSql = "INSERT INTO migration_lock (id, locked, locked_at, locked_by) VALUES (1, TRUE, CURRENT_TIMESTAMP, ?)";
        String updateSql = "UPDATE migration_lock SET locked = TRUE, locked_at = CURRENT_TIMESTAMP, locked_by = ? WHERE id = 1";

        try (Statement checkStatement = connection.createStatement();
             ResultSet resultSet = checkStatement.executeQuery(checkSql)) {
            resultSet.next();
            boolean exists = resultSet.getInt(1) > 0;

            try (PreparedStatement preparedStatement = connection.prepareStatement(exists ? updateSql : insertSql)) {
                preparedStatement.setString(1, lockedBy);
                preparedStatement.executeUpdate();
            }
        }
    }
    /**
     * Разблокирует процесс миграции, обновляя статус блокировки в таблице `migration_lock`.
     * Этот метод устанавливает столбец `locked` в `FALSE` и очищает поля `locked_at` и `locked_by`,
     * указывая, что процесс миграции больше не выполняется.
     *
     * <p>Использование: Вызовите этот метод в конце процесса миграции, чтобы снять блокировку
     * и позволить другим процессам выполнять миграцию.</p>
     *
     * @throws SQLException, если при взаимодействии с базой данных произошла ошибка.
     */
    public void unlockMigration() throws SQLException {
        String sql = "UPDATE migration_lock SET locked = FALSE, locked_at = NULL, locked_by = NULL WHERE id = 1";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
    /**
     * Применяет миграцию к базе данных.
     * <p>
     * Этот метод выполняет предоставленный сценарий SQL и обновляет таблицу отслеживания схем метаданными миграции.
     * </p>
     *
     * @param version версия миграции
     * @param description краткое описание миграции
     * @param sql сценарий SQL для выполнения миграции
     * @param rollbackFile имя файла отката, связанного с этой миграцией
     * @throws SQLException если при применении миграции возникла ошибка базы данных
     */
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
    /**
     * Откатывает миграцию из базы данных.
     * <p>
     * Этот метод выполняет предоставленный SQL-сценарий для отмены изменений, внесенных миграцией.
     * </p>
     *
     * @param version версия миграции для отката
     * @param description краткое описание цели отката
     * @param sql сценарий SQL, который необходимо выполнить для отката
     * @throws SQLException, если при применении отката возникла ошибка базы данных
     */
    public void rollbackMigration( String version, String description, String sql) throws SQLException {
        logger.info("Rolling back version: " + sql);
        logger.info("SQL to execute: " + sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(description);
        }
        logger.info("Rollback SQL applied for version: " + sql);
    }
}


