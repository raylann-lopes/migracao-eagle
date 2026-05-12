package com.example.demo.migration.service;

public record RowIssue(
        int rowNumber,
        String field,
        String message,
        String severity) {
}
