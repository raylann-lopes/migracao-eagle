package com.example.demo.migration.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMigrationProcessRequestDTO(
        @NotBlank @Size(max = 120) String clientName,
        @Size(max = 18) String cnpj,
        @NotBlank @Size(max = 30) String eagleVersion,
        @Valid @NotNull MigrationConfigRequestDTO config) {
}
