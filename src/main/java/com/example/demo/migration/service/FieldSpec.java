package com.example.demo.migration.service;

import com.example.demo.migration.domain.FieldType;

public record FieldSpec(
        String name,
        FieldType type,
        int maxLength,
        boolean required,
        String description) {
}
