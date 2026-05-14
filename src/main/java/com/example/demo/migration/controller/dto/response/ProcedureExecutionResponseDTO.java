package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.ProcedureExecutionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcedureExecutionResponseDTO(
        UUID id,
        int stepOrder,
        String procedureName,
        ProcedureExecutionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage) {
}
