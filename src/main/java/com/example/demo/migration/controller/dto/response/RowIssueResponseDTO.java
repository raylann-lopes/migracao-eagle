package com.example.demo.migration.controller.dto.response;

public record RowIssueResponseDTO(
        int rowNumber,
        String field,
        String message,
        String severity) {
}
