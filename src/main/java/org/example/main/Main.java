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

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws FileNotFoundException {
        PropertiesUtils config = new PropertiesUtils();
        ConnectionManager connectionManager = new ConnectionManager(config);
        MigrationFileReader migrationFileReader = new MigrationFileReader();


        try {


            Connection connection = connectionManager.connect();
            logger.info("db is connected");
            MigrationExecutor migrationExecutor = new MigrationExecutor(connection, new MigrationFileReader());
            MigrationTool migrationTool = new MigrationTool(migrationExecutor, connection);

            migrationTool.executeMigration();
            migrationTool.executeRollback();
            connection.close();
            logger.info("connection is closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
