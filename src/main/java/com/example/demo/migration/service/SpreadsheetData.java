package com.example.demo.migration.service;

import java.util.List;

public record SpreadsheetData(
        List<String> headers,
        List<SheetRow> rows) {
}
