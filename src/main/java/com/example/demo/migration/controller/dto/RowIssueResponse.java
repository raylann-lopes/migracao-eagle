package com.example.demo.migration.controller.dto;

public record RowIssueResponse(
        int rowNumber,
        String field,
        String message,
        String severity) {
}
