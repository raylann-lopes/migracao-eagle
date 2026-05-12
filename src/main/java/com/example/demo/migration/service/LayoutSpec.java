package com.example.demo.migration.service;

import com.example.demo.migration.domain.MigrationModule;
import java.util.List;

public record LayoutSpec(
        MigrationModule module,
        String targetTable,
        String keyField,
        List<FieldSpec> fields) {
}
