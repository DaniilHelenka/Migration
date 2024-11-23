package org.example.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
/**
 * Утилитарный класс для загрузки и доступа к свойствам приложения.
 * <p>
 * Класс PropertiesUtils считывает свойства из файла application.properties
 * расположенного в каталоге ресурсов. Он предоставляет методы для получения определенных свойств,
 * связанных с базой данных.
 * </p>
 */
public class PropertiesUtils {
    private final Properties properties = new Properties();
    /**
     * Загружает файл  application.properties из classpath и инициализирует объект свойств.
     * <p>
     * Если файл не найден или при загрузке произошла ошибка, этот конструктор выбросит исключение времени выполнения.
     * </p>
     *
     * @throws RuntimeException, если файл свойств отсутствует или не может быть загружен
     */
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