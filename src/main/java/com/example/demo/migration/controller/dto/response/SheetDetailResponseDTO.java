package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.MigrationModule;
import java.util.List;
import java.util.Map;

public record SheetDetailResponseDTO(
        MigrationModule module,
        SheetSummaryResponseDTO summary,
        List<Map<String, String>> previewRows,
        List<RowIssueResponseDTO> issues) {
}
