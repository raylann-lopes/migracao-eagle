package com.example.demo.migration.service;

import java.util.List;

public record ValidationResult(
        List<ValidatedRow> rows,
        List<RowIssue> layoutIssues) {

    public int totalRows() {
        return rows.size();
    }

    public int validRows() {
        return (int) rows.stream().filter(ValidatedRow::valid).count();
    }

    public int errorCount() {
        return layoutIssues.size() + rows.stream().mapToInt(row -> row.errors().size()).sum();
    }

    public int warningCount() {
        return rows.stream().mapToInt(row -> row.warnings().size()).sum();
    }
}
