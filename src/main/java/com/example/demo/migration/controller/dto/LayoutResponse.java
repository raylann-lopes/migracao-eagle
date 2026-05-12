package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.MigrationModule;
import java.util.List;

public record LayoutResponse(
        MigrationModule module,
        String targetTable,
        List<LayoutFieldResponse> fields) {
}
