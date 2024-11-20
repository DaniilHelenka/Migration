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

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String username = "techtask";
        String password = "techtask";

        try {
            // Создание подключения
            DatabaseConfig dbConfig = new DatabaseConfig(url, username, password);
            Connection connection = dbConfig.getConnection();

            // Инициализация истории миграций

            MigrationService migrationService = new MigrationService(connection);

            // Применение миграций

            MigrateCommand migrateCommand = new MigrateCommand(migrationService);
            migrateCommand.execute();

            int currentVersion = migrationService.getCurrentVersion();
            System.out.println("Current version: " + currentVersion);




            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}