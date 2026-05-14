package com.example.demo.migration.service;

import com.example.demo.migration.controller.dto.response.LayoutFieldResponseDTO;
import com.example.demo.migration.controller.dto.response.LayoutResponseDTO;
import com.example.demo.migration.controller.dto.response.MigrationConfigResponseDTO;
import com.example.demo.migration.controller.dto.response.MigrationProcessResponseDTO;
import com.example.demo.migration.controller.dto.response.ProcedureExecutionResponseDTO;
import com.example.demo.migration.controller.dto.response.SheetSummaryResponseDTO;
import com.example.demo.migration.domain.MigrationProcessEntity;
import com.example.demo.migration.domain.MigrationSheetEntity;
import com.example.demo.migration.domain.ProcedureExecutionEntity;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class MigrationMapper {

    public MigrationProcessResponseDTO toResponse(MigrationProcessEntity process) {
        return new MigrationProcessResponseDTO(
                process.getId(),
                process.getClientName(),
                process.getCnpj(),
                process.getEagleVersion(),
                null,
                null,
                process.getFinalDatabaseFilename(),
                process.getFinalDatabasePath() != null,
                process.getStatus(),
                new MigrationConfigResponseDTO(
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

    public SheetSummaryResponseDTO toSheetSummary(MigrationSheetEntity sheet) {
        return new SheetSummaryResponseDTO(
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

    public ProcedureExecutionResponseDTO toProcedureResponse(ProcedureExecutionEntity execution) {
        return new ProcedureExecutionResponseDTO(
                execution.getId(),
                execution.getStepOrder(),
                execution.getProcedureName(),
                execution.getStatus(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getErrorMessage());
    }

    public LayoutResponseDTO toLayoutResponseDTO(LayoutSpec layout) {
        return new LayoutResponseDTO(
                layout.module(),
                layout.targetTable(),
                layout.fields().stream()
                        .map(field -> new LayoutFieldResponseDTO(
                                field.name(),
                                field.type(),
                                field.maxLength(),
                                field.required(),
                                field.description()))
                        .toList());
    }
}
