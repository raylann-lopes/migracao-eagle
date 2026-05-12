package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.domain.MigrationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SheetSummaryResponse(
        UUID id,
        MigrationModule module,
        MigrationStatus status,
        String originalFilename,
        int totalRows,
        int validRows,
        int errorCount,
        int warningCount,
        OffsetDateTime uploadedAt,
        OffsetDateTime validatedAt,
        OffsetDateTime importedAt) {
}
