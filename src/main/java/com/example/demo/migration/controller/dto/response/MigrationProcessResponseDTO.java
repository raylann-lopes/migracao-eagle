package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.MigrationStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record MigrationProcessResponseDTO(
        Long id,
        String clientName,
        String cnpj,
        String eagleVersion,
        String eagleWorkingDatabasePath,
        String finalDatabasePath,
        String finalDatabaseFilename,
        boolean finalDatabaseAvailable,
        MigrationStatus status,
        MigrationConfigResponseDTO config,
        List<SheetSummaryResponseDTO> sheets,
        List<ProcedureExecutionResponseDTO> procedures,
        String lastError,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
