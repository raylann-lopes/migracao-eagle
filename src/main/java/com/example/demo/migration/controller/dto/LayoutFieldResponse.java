package com.example.demo.migration.controller.dto;

import com.example.demo.migration.domain.FieldType;

public record LayoutFieldResponse(
        String name,
        FieldType type,
        int maxLength,
        boolean required,
        String description) {
}
