package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.MigrationModule;
import java.util.List;

public record LayoutResponseDTO(
        MigrationModule module,
        String targetTable,
        List<LayoutFieldResponseDTO> fields) {
}
