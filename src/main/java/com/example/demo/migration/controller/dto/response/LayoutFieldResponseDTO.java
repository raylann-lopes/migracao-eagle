package com.example.demo.migration.controller.dto.response;

import com.example.demo.migration.domain.FieldType;

public record LayoutFieldResponseDTO(
        String name,
        FieldType type,
        int maxLength,
        boolean required,
        String description) {
}
