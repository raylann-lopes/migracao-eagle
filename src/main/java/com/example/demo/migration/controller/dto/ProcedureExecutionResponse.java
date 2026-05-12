package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.ProcedureExecutionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcedureExecutionResponse(
        UUID id,
        int stepOrder,
        String procedureName,
        ProcedureExecutionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage) {
}
