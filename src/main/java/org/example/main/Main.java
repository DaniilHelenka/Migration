package org.example.main;

import org.example.configuration.PropertiesUtils;
import org.example.db.ConnectionManager;
import org.example.migrations.MigrationExecutor;
import org.example.migrations.MigrationFileReader;
import org.example.migrations.MigrationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.sql.Connection;
/**
 * Точка входа в приложение.
 * <p>
 * Класс Main инициализирует и управляет основными компонентами, необходимыми для выполнения миграций и откатов базы данных.
 * Это:
 * - Загружает свойства конфигурации.
 * - Устанавливает соединение с базой данных.
 * - Выполняет миграции и откаты с помощью MigrationTool.
 * </p>
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    /**
     * Главный метод приложения.
     * <p>
     * Этот метод устанавливает конфигурацию, инициализирует подключение к базе данных и средства миграции, а также выполняет
     * миграцию и откат базы данных. Для отслеживания прогресса и обработки возможных ошибок генерируются журналы.
     * </p>
     *
     * @param args аргументы командной строки (не используются в данном приложении)
     */
    public static void main(String[] args) {

        PropertiesUtils config = new PropertiesUtils();
        ConnectionManager connectionManager = new ConnectionManager(config);
        MigrationFileReader migrationFileReader = new MigrationFileReader();

        try {

            // Установите соединение с базой данных
            Connection connection = connectionManager.connect();
            logger.info("db is connected");

            // Инициализация инструментов миграции
            MigrationExecutor migrationExecutor = new MigrationExecutor(connection, new MigrationFileReader());
            MigrationTool migrationTool = new MigrationTool(migrationExecutor, connection);

            // Выполнение миграций и откатов
            migrationTool.executeMigration();
            migrationTool.executeRollback();

            // Закрытие соединения
            connection.close();
            logger.info("connection is closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
