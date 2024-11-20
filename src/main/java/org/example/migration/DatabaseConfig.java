package org.example.migration;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private String url;
    private String username;
    private String password;

    public DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    public static DatabaseConfig loadFromProperties(String propertiesFileName) {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
            if (input == null) {
                throw new IOException("Файл конфигурации не найден: " + propertiesFileName);
            }
            properties.load(input);

            String url = properties.getProperty("database.url");
            String username = properties.getProperty("database.username");
            String password = properties.getProperty("database.password");

            if (url == null || username == null || password == null) {
                throw new IllegalArgumentException("Некоторые параметры подключения отсутствуют в " + propertiesFileName);
            }

            return new DatabaseConfig(url, username, password);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки конфигурации из файла: " + e.getMessage(), e);
        }
    }
}
