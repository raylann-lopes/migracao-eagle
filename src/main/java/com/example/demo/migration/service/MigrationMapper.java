package com.example.demo.migration.service;

import com.example.demo.migration.controller.dto.LayoutFieldResponse;
import com.example.demo.migration.controller.dto.LayoutResponse;
import com.example.demo.migration.controller.dto.MigrationConfigResponse;
import com.example.demo.migration.controller.dto.MigrationProcessResponse;
import com.example.demo.migration.controller.dto.ProcedureExecutionResponse;
import com.example.demo.migration.controller.dto.SheetSummaryResponse;
import com.example.demo.migration.domain.MigrationProcessEntity;
import com.example.demo.migration.domain.MigrationSheetEntity;
import com.example.demo.migration.domain.ProcedureExecutionEntity;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class MigrationMapper {

    public MigrationProcessResponse toResponse(MigrationProcessEntity process) {
        return new MigrationProcessResponse(
                process.getId(),
                process.getClientName(),
                process.getCnpj(),
                process.getEagleVersion(),
                null,
                null,
                process.getFinalDatabaseFilename(),
                process.getFinalDatabasePath() != null,
                process.getStatus(),
                new MigrationConfigResponse(
                        process.getDefaultDistrictId(),
                        process.getDefaultCep(),
                        process.getCompanyState(),
                        process.isMigrateReceivables()),
                process.getSheets().stream()
                        .sorted(Comparator.comparing(sheet -> sheet.getModule().name()))
                        .map(this::toSheetSummary)
                        .toList(),
                process.getProcedureExecutions().stream()
                        .sorted(Comparator.comparingInt(ProcedureExecutionEntity::getStepOrder))
                        .map(this::toProcedureResponse)
                        .toList(),
                process.getLastError(),
                process.getCreatedAt(),
                process.getUpdatedAt());
    }

    public SheetSummaryResponse toSheetSummary(MigrationSheetEntity sheet) {
        return new SheetSummaryResponse(
                sheet.getId(),
                sheet.getModule(),
                sheet.getStatus(),
                sheet.getOriginalFilename(),
                sheet.getTotalRows(),
                sheet.getValidRows(),
                sheet.getErrorCount(),
                sheet.getWarningCount(),
                sheet.getUploadedAt(),
                sheet.getValidatedAt(),
                sheet.getImportedAt());
    }

    public ProcedureExecutionResponse toProcedureResponse(ProcedureExecutionEntity execution) {
        return new ProcedureExecutionResponse(
                execution.getId(),
                execution.getStepOrder(),
                execution.getProcedureName(),
                execution.getStatus(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getErrorMessage());
    }

    public LayoutResponse toLayoutResponse(LayoutSpec layout) {
        return new LayoutResponse(
                layout.module(),
                layout.targetTable(),
                layout.fields().stream()
                        .map(field -> new LayoutFieldResponse(
                                field.name(),
                                field.type(),
                                field.maxLength(),
                                field.required(),
                                field.description()))
                        .toList());
    }
}
