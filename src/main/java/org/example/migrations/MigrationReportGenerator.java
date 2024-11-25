package org.example.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
/**
 * Класс-утилита для создания отчетов о миграции в форматах CSV и JSON.
 * <p>
 * Этот класс предоставляет методы для создания отчетов из списка объектов MigrationReport
 * и сохранять их в файлы в нужном формате.
 * </p>
 */

public class MigrationReportGenerator {
    // Генерация CSV
    public void generateCsvReport(List<MigrationReport> reports, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Version,Description,Success,Timestamp,ErrorMessage\n");
            for (MigrationReport report : reports) {
                writer.append(report.getVersion()).append(", ")
                        .append(report.getDescription()).append(", ")
                        .append(String.valueOf(report.isSuccess())).append("," )
                        .append(report.getTimestamp()).append(", ")
                        .append(report.getErrorMessage() != null ? report.getErrorMessage() : "").append("\n");
            }
        }
    }

    // Генерация JSON
    public void generateJsonReport(List<MigrationReport> reports, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new FileWriter(filePath), reports);
    }
}