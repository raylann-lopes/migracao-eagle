package com.example.demo.migration.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MigrationSpreadsheetReader {

    public SpreadsheetData read(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".csv")) {
                return readCsv(file);
            }
            if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
                return readExcel(file);
            }
        } catch (IOException | CsvException exception) {
            throw new IllegalArgumentException("Nao foi possivel ler a planilha: " + exception.getMessage(), exception);
        }
        throw new IllegalArgumentException("Formato nao suportado. Envie .xls, .xlsx ou .csv.");
    }

    private SpreadsheetData readCsv(MultipartFile file) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> lines = reader.readAll();
            if (lines.isEmpty()) {
                return new SpreadsheetData(List.of(), List.of());
            }
            List<String> headers = normalizeHeaders(Arrays.asList(lines.getFirst()));
            List<SheetRow> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                Map<String, String> values = new LinkedHashMap<>();
                String[] line = lines.get(index);
                for (int column = 0; column < headers.size(); column++) {
                    values.put(headers.get(column), column < line.length ? trimToNull(line[column]) : null);
                }
                if (values.values().stream().anyMatch(value -> value != null && !value.isBlank())) {
                    rows.add(new SheetRow(index + 1, values));
                }
            }
            return new SpreadsheetData(headers, rows);
        }
    }

    private SpreadsheetData readExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new SpreadsheetData(List.of(), List.of());
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(normalizeHeader(formatter.formatCellValue(cell)));
            }

            List<SheetRow> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    Cell cell = row.getCell(column);
                    values.put(headers.get(column), cell == null ? null : trimToNull(formatter.formatCellValue(cell)));
                }
                if (values.values().stream().anyMatch(value -> value != null && !value.isBlank())) {
                    rows.add(new SheetRow(rowIndex + 1, values));
                }
            }
            return new SpreadsheetData(headers, rows);
        }
    }

    private List<String> normalizeHeaders(List<String> headers) {
        return headers.stream().map(this::normalizeHeader).toList();
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
