package org.example.migrations.fileReader;
/**
 * Представляет файл миграции или отката с версией, описанием и содержимым SQL.
 * <p>
 * Этот класс инкапсулирует метаданные и содержимое файла миграции, включая:
 * - Версию миграции
 * - Описание
 * - SQL-сценарий, который будет выполняться для миграции или отката
 * </p>
 */
public class MigrationFile {
    private final String version;
    private final String description;
    private final String sql;
    /**
     * Создает экземпляр MigrationFile с указанной версией, описанием и содержимым SQL.
     *
     * @param version версия миграции, обычно извлекаемая из имени файла
     * @param description краткое описание цели миграции
     * @param sql сценарий SQL, связанный с миграцией или откатом.
     */
    public MigrationFile(String version, String description, String sql) {
        this.version = version;
        this.description = description;
        this.sql = sql;
    }
    /**
     * Возвращает версию миграции.
     *
     * @return версия миграции в виде строки
     */
    public String getVersion() {
        return version;
    }
    /**
     * Возвращает описание миграции.
     *
     * @return краткое описание миграции
     */
    public String getDescription() {
        return description;
    }
    /**
     * Возвращает SQL-содержимое миграции.
     *
     * @return SQL-скрипт, связанный с миграцией или откатом.
     */
    public String getSql() {
        return sql;
    }
}
