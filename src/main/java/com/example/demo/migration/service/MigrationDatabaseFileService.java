package com.example.demo.migration.service;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.MigrationProcessEntity;
import com.example.demo.migration.exception.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class MigrationDatabaseFileService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MigrationProperties properties;
    private final S3Client s3Client;

    public MigrationDatabaseFileService(MigrationProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public PreparedDatabase prepareFinalDatabase(MigrationProcessEntity process) {
        Path workingDatabase = Path.of(properties.getEagleWorkingDatabasePath()).toAbsolutePath().normalize();
        String filename = finalFilename(process);
        Path finalDatabase = Path.of(properties.getFinalDatabaseOutputDir()).toAbsolutePath().normalize().resolve(filename);

        try {
            Files.createDirectories(workingDatabase.getParent());
            Files.createDirectories(finalDatabase.getParent());
            copyCleanDatabaseTemplate(process.getCleanDatabasePath(), workingDatabase);
            return new PreparedDatabase(workingDatabase, finalDatabase, filename);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao preparar banco limpo do Eagle: " + exception.getMessage(), exception);
        }
    }

    public void publishFinalDatabase(PreparedDatabase preparedDatabase) {
        try {
            Files.copy(preparedDatabase.workingDatabasePath(), preparedDatabase.finalDatabasePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao gerar banco final para download: " + exception.getMessage(), exception);
        }
    }

    public Resource finalDatabaseResource(MigrationProcessEntity process) {
        if (process.getFinalDatabasePath() == null || process.getFinalDatabasePath().isBlank()) {
            throw new BusinessException("Banco final ainda nao foi gerado.");
        }
        Path finalPath = Path.of(process.getFinalDatabasePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(finalPath)) {
            throw new BusinessException("Arquivo final nao encontrado: " + finalPath);
        }
        return new FileSystemResource(finalPath);
    }

    private String finalFilename(MigrationProcessEntity process) {
        String client = Normalizer.normalize(process.getClientName(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase(Locale.ROOT);
        if (client.isBlank()) {
            client = "cliente";
        }
        return client + "-" + FILE_DATE.format(OffsetDateTime.now()) + ".FDB";
    }

    private void copyCleanDatabaseTemplate(String templateReference, Path workingDatabase) throws IOException {
        if (templateReference != null && templateReference.startsWith("s3://")) {
            downloadFromS3(templateReference, workingDatabase);
            return;
        }

        Path cleanTemplate = Path.of(templateReference).toAbsolutePath().normalize();
        if (!Files.isRegularFile(cleanTemplate)) {
            throw new BusinessException("Banco limpo do Eagle nao encontrado: " + cleanTemplate);
        }
        Files.copy(cleanTemplate, workingDatabase, StandardCopyOption.REPLACE_EXISTING);
    }

    private void downloadFromS3(String storageUri, Path target) {
        URI uri = URI.create(storageUri);
        String bucket = uri.getHost();
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (bucket == null || bucket.isBlank() || key.isBlank()) {
            throw new BusinessException("Template S3 do banco limpo invalido: " + storageUri);
        }
        try {
            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    ResponseTransformer.toFile(target));
        } catch (SdkException exception) {
            throw new BusinessException("Falha ao baixar banco limpo do S3: " + exception.getMessage(), exception);
        }
    }
}
