package org.example.main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.PropertiesUtils;
import org.example.db.ConnectionManager;
import org.example.migrations.MigrationExecutor;
import org.example.migrations.fileReader.MigrationFileReader;
import org.example.migrations.MigrationTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

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
@Slf4j
@RequiredArgsConstructor
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
            //CLI
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter command (migrate/rollback/status/exit): ");
                String command = scanner.nextLine().trim().toLowerCase();

                switch (command) {
                    case "migrate":
                        try {
                            migrationTool.executeMigration();
                            System.out.println("Migrations applied successfully.");
                        } catch (SQLException e) {
                            System.err.println("Migration failed: " + e.getMessage());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;

                    case "rollback":
                        try {
                            migrationTool.executeRollback();
                            System.out.println("Rollback completed successfully.");
                        } catch (SQLException e) {
                            System.err.println("Rollback failed: " + e.getMessage());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;

                    case "status":
                        try {
                            String currentVersion = migrationExecutor.getCurrentVersion();
                            System.out.println("Current database version: " + currentVersion);
                        } catch (SQLException e) {
                            System.err.println("Failed to retrieve database status: " + e.getMessage());
                        }
                        break;

                    case "exit":
                        System.out.println("Exiting...");
                        return;

                    default:
                        System.out.println("Unknown command. Please try again.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }


}
