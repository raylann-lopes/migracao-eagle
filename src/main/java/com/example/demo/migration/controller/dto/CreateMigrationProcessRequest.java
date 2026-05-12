package com.example.demo.migration.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMigrationProcessRequest(
        @NotBlank @Size(max = 120) String clientName,
        @Size(max = 18) String cnpj,
        @NotBlank @Size(max = 30) String eagleVersion,
        @Valid @NotNull MigrationConfigRequest config) {
}
