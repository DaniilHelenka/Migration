package org.example;

import org.example.migration.DatabaseConfig;
import org.example.migration.MigrateCommand;
import org.example.migration.MigrationService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");


        // Создание подключения
        try {
            // Загрузка конфигурации из application.properties
            DatabaseConfig config = DatabaseConfig.loadFromProperties("application.properties");

            // Установка соединения с базой данных
            try (Connection connection = config.getConnection()) {
                System.out.println("Соединение установлено успешно!");
                // Инициализация истории миграций
                MigrationService migrationService = new MigrationService(connection);
                // Применение миграций

                MigrateCommand migrateCommand = new MigrateCommand(migrationService);
                migrateCommand.execute();

                int currentVersion = migrationService.getCurrentVersion();
                System.out.println("Current version: " + currentVersion);
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }
}