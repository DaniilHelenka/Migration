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


