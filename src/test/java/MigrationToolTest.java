import org.example.migrations.MigrationExecutor;
import org.example.migrations.fileReader.MigrationFileReader;
import org.example.migrations.MigrationTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class MigrationToolTest {

    private MigrationTool migrationTool;
    private MigrationExecutor migrationExecutor;
    private MigrationFileReader migrationFileReader;
    String url = "jdbc:postgresql://localhost:5432/postgres";
    String username = "techtask";
    String password = "techtask";
    Connection connection = DriverManager.getConnection(url, username, password);

    public MigrationToolTest() throws SQLException {
    }

    @BeforeEach
    public void setUp() throws SQLException {


        // Создаем миграционный исполнитель с двумя аргументами
        migrationFileReader = new MigrationFileReader();
        migrationExecutor = new MigrationExecutor(connection, migrationFileReader);

        // Создаем объект MigrationTool
        migrationTool = new MigrationTool(migrationExecutor, connection);

        // Инициализация базы данных и таблиц для теста
        migrationExecutor.initializeSchemaTable();
        migrationExecutor.initializeMigrationLockTable();
    }

    @Test
    public void testExecuteMigration() throws SQLException, IOException {
        // Выполняем миграцию
        migrationTool.executeMigration();

        // Проверяем, что миграции применены (например, проверяем версии в таблице миграций)
        String currentVersion = migrationExecutor.getCurrentVersion();
        assertNotNull(currentVersion, "Current version should not be null.");
        assertEquals("3", currentVersion, "Expected version is 3 after migration.");

        // Проверяем создание отчетов
        File csvReport = new File("src/main/resources/reports/migration_report.csv");
        File jsonReport = new File("src/main/resources/reports/migration_report.json");

        assertTrue(csvReport.exists(), "CSV report file should exist.");
        assertTrue(jsonReport.exists(), "JSON report file should exist.");
    }

    // Решить проблему с выбоором версии отката в тесте(


   /* @Test
    public void testExecuteRollback() throws SQLException, IOException {

        // Создаем объект MigrationTool
        MigrationTool migrationTool = new MigrationTool(migrationExecutor, connection);

        // Инициализация базы данных и таблиц для теста
        migrationExecutor.initializeSchemaTable();
        migrationExecutor.initializeMigrationLockTable();

        // Выполняем миграцию, чтобы база данных имела версию
        migrationTool.executeMigration();

        // Выполняем откат на предыдущую версию
        migrationTool.executeRollback();

        // Проверяем, что версия базы данных откатилась
        String currentVersion = migrationExecutor.getCurrentVersion();
        assertNotNull(currentVersion);
        assertEquals("1", currentVersion); // Версия после отката (например, "1")
    }*/
}

