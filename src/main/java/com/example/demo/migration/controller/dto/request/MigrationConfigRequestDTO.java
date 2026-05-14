package com.example.demo.migration.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MigrationConfigRequestDTO(
        Integer defaultDistrictId,
        String defaultCep,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String companyState,
        boolean migrateReceivables) {
}
