package com.example.demo.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.MigrationProcessEntity;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class MigrationDatabaseFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should prepare working database from S3 template and publish final file to S3")
    void shouldPrepareWorkingDatabaseFromS3AndPublishFinalFile() throws Exception {
        MigrationProperties properties = new MigrationProperties();
        properties.setWorkDir(tempDir.resolve("work").toString());
        properties.getFinalDatabase().getS3().setBucket("eagle-final");
        properties.getFinalDatabase().getS3().setPrefix("bancos-migrados");

        MigrationProcessEntity process = new MigrationProcessEntity();
        process.setClientName("Cliente Teste");
        process.setEagleVersion("2025.002");
        process.setCleanDatabasePath("s3://eagle-templates/bancos-limpos/2025.002/EAGLEERP_LIMPO.FDB");

        MigrationDatabaseFileService service = new MigrationDatabaseFileService(properties, s3Client());
        PreparedDatabase prepared = service.prepareFinalDatabase(process);

        assertThat(Files.readString(prepared.workingDatabasePath())).isEqualTo("banco-limpo");

        Files.writeString(prepared.workingDatabasePath(), "banco-migrado");

        String storageUri = service.publishFinalDatabase(prepared);

        assertThat(prepared.finalDatabaseFilename()).startsWith("cliente-teste-").endsWith(".FDB");
        assertThat(Files.readString(prepared.finalDatabasePath())).isEqualTo("banco-migrado");
        assertThat(storageUri).startsWith("s3://eagle-final/bancos-migrados/2025.002/");
    }

    private S3Client s3Client() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.getObject(
                any(GetObjectRequest.class),
                any(ResponseTransformer.class))).thenAnswer(invocation -> {
            ResponseTransformer<GetObjectResponse, Path> transformer = invocation.getArgument(1);
            return transformer.transform(
                    GetObjectResponse.builder().build(),
                    AbortableInputStream.create(new ByteArrayInputStream("banco-limpo".getBytes(StandardCharsets.UTF_8))));
        });
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        return s3Client;
    }
}
