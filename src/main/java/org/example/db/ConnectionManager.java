package org.example.db;

import org.example.configuration.PropertiesUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * Управляет соединениями с базами данных с помощью свойств конфигурации.
 * <p>
 * Класс ConnectionManager предоставляет метод для установки соединения с базой данных.
 * используя учетные данные и URL, указанные в конфигурации PropertiesUtils.
 * </p>
 */
public class ConnectionManager {
    private final PropertiesUtils config;
    /**
     * Конструирует экземпляр  ConnectionManager с указанной утилитой конфигурации.
     *
     * @param config экземпляр PropertiesUtils, предоставляющий свойства подключения к базе данных
     */
    public ConnectionManager(PropertiesUtils config) {

        this.config=config;
    }
    /**
     * Устанавливает соединение с базой данных.
     * <p>
     * Свойства соединения, такие как URL, имя пользователя и пароль,
     * извлекаются из предоставленного PropertiesUtils.
     * </p>
     *
     * @return объект Connection, представляющий соединение с базой данных.
     * @throws SQLException при возникновении ошибки доступа к базе данных
     */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }
}
