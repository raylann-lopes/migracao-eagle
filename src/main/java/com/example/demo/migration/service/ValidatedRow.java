package com.example.demo.migration.service;

import java.util.List;
import java.util.Map;

public record ValidatedRow(
        int rowNumber,
        Map<String, String> normalizedValues,
        List<RowIssue> errors,
        List<RowIssue> warnings) {

    public boolean valid() {
        return errors.isEmpty();
    }
}
