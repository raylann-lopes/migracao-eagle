package com.example.demo.migration.controller.dto;

public record MigrationConfigResponse(
        Integer defaultDistrictId,
        String defaultCep,
        String companyState,
        boolean migrateReceivables) {
}
