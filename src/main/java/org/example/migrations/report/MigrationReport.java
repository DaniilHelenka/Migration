package org.example.migrations.report;
/**
 * Представляет отчет по одной операции миграции, включая ее детали и результат.
 * <p>
 * Этот класс хранит информацию о миграции, такую как версия, описание,
 * была ли миграция успешной, временная метка операции и сообщение об ошибке, если применимо.
 * Он используется в основном для создания отчетов о миграции в форматах JSON или CSV.
 * </p>
 */
public class MigrationReport {
    private final String version;
    private final String description;
    private final boolean success;
    private final String timestamp;
    private final String errorMessage;

    public MigrationReport(String version, String description, boolean success, String timestamp, String errorMessage) {
        this.version = version;
        this.description = description;
        this.success = success;
        this.timestamp = timestamp;
        this.errorMessage = errorMessage;
    }
    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}