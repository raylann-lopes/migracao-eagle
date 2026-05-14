package com.example.demo.migration.controller.dto.response;

public record MigrationConfigResponseDTO(
        Integer defaultDistrictId,
        String defaultCep,
        String companyState,
        boolean migrateReceivables) {
}
