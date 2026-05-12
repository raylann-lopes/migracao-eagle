package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.MigrationModule;
import java.util.List;
import java.util.Map;

public record SheetDetailResponse(
        MigrationModule module,
        SheetSummaryResponse summary,
        List<Map<String, String>> previewRows,
        List<RowIssueResponse> issues) {
}
