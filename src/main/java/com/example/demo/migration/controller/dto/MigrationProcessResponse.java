package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.MigrationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MigrationProcessResponse(
        UUID id,
        String clientName,
        String cnpj,
        String eagleVersion,
        String eagleWorkingDatabasePath,
        String finalDatabasePath,
        String finalDatabaseFilename,
        boolean finalDatabaseAvailable,
        MigrationStatus status,
        MigrationConfigResponse config,
        List<SheetSummaryResponse> sheets,
        List<ProcedureExecutionResponse> procedures,
        String lastError,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
