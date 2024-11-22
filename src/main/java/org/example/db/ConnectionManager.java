package org.example.db;

import org.example.configuration.PropertiesUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private final PropertiesUtils config;

    public ConnectionManager(PropertiesUtils config) {

        this.config=config;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }
}
