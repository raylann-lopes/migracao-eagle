package com.example.demo.migration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.MigrationProcessEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationDatabaseFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should prepare working database from clean template and publish final file")
    void shouldPrepareWorkingDatabaseAndPublishFinalFile() throws Exception {
        Path template = tempDir.resolve("EAGLE_LIMPO.FDB");
        Files.writeString(template, "banco-limpo");

        MigrationProperties properties = new MigrationProperties();
        properties.setEagleWorkingDatabasePath(tempDir.resolve("work/EAGLEERP.FDB").toString());
        properties.setFinalDatabaseOutputDir(tempDir.resolve("final").toString());

        MigrationProcessEntity process = new MigrationProcessEntity();
        process.setClientName("Cliente Teste");
        process.setCleanDatabasePath(template.toString());

        MigrationDatabaseFileService service = new MigrationDatabaseFileService(properties, null);
        PreparedDatabase prepared = service.prepareFinalDatabase(process);
        Files.writeString(prepared.workingDatabasePath(), "banco-migrado");

        service.publishFinalDatabase(prepared);

        assertThat(prepared.finalDatabaseFilename()).startsWith("cliente-teste-").endsWith(".FDB");
        assertThat(Files.readString(prepared.finalDatabasePath())).isEqualTo("banco-migrado");
    }
}
