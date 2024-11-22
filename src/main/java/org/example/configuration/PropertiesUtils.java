package org.example.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtils {
    private final Properties properties = new Properties();

    public PropertiesUtils() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties file not found in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    public String getUrl() {
        return properties.getProperty("db.url");
    }

    public String getUsername() {
        return properties.getProperty("db.username");
    }

    public String getPassword() {
        return properties.getProperty("db.password");
    }
}