package com.example.demo.migration.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MigrationConfigRequest(
        Integer defaultDistrictId,
        @NotBlank String defaultCep,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String companyState,
        boolean migrateReceivables) {
}
