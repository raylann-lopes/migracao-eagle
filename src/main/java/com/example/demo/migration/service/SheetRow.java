package com.example.demo.migration.service;

import java.util.Map;

public record SheetRow(
        int rowNumber,
        Map<String, String> values) {
}
