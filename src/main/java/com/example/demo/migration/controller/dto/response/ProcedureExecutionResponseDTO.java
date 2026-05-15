package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.ProcedureExecutionStatus;
import java.time.OffsetDateTime;

public record ProcedureExecutionResponseDTO(
        Long id,
        int stepOrder,
        String procedureName,
        ProcedureExecutionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage) {
}
