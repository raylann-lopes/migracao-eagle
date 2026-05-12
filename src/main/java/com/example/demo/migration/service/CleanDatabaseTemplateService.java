package com.example.demo.migration.service;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.controller.dto.CleanDatabaseTemplateResponse;
import com.example.demo.migration.exception.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class CleanDatabaseTemplateService {

    private final MigrationProperties properties;
    private final S3Client s3Client;

    public CleanDatabaseTemplateService(MigrationProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public List<CleanDatabaseTemplateResponse> list() {
        List<CleanDatabaseTemplateResponse> templates = properties.getCleanDatabase().getTemplates().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CleanDatabaseTemplateResponse::version))
                .toList();
        if (!templates.isEmpty()) {
            return templates;
        }
        return List.of();
    }

    public String templateReferenceForVersion(String version) {
        MigrationProperties.Template template = templateForVersion(version);
        return storageUri(template);
    }

    public Path localTemplatePathForVersion(String version) {
        String reference = templateReferenceForVersion(version);
        Path cachedPath = Path.of(properties.getCleanDatabase().getCacheDir())
                .toAbsolutePath()
                .normalize()
                .resolve(version)
                .resolve("EAGLEERP_LIMPO.FDB");
        if (Files.isRegularFile(cachedPath)) {
            return cachedPath;
        }
        downloadFromS3(reference, cachedPath);
        return cachedPath;
    }

    private MigrationProperties.Template templateForVersion(String version) {
        if (properties.getCleanDatabase().getTemplates().isEmpty()) {
            throw new BusinessException("Nenhum banco limpo foi configurado no ambiente AWS.");
        }
        return properties.getCleanDatabase().getTemplates().stream()
                .filter(template -> template.getVersion().equals(version))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Banco limpo da versao " + version + " nao configurado no ambiente."));
    }

    private CleanDatabaseTemplateResponse toResponse(MigrationProperties.Template template) {
        String description = isBlank(template.getDescription())
                ? "Eagle Gestao " + template.getVersion()
                : template.getDescription();
        return new CleanDatabaseTemplateResponse(template.getVersion(), description);
    }

    private String storageUri(MigrationProperties.Template template) {
        if (!isBlank(template.getS3Key())) {
            String bucket = properties.getCleanDatabase().getS3().getBucket();
            return "s3://" + bucket + "/" + template.getS3Key();
        }
        throw new BusinessException("Banco limpo da versao " + template.getVersion() + " nao possui chave S3 configurada.");
    }

    private void downloadFromS3(String storageUri, Path target) {
        URI uri = URI.create(storageUri);
        String bucket = uri.getHost();
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (bucket == null || bucket.isBlank() || key.isBlank()) {
            throw new BusinessException("Template S3 do banco limpo invalido: " + storageUri);
        }
        try {
            Files.createDirectories(target.getParent());
            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    ResponseTransformer.toFile(target));
        } catch (IOException | SdkException exception) {
            throw new BusinessException("Falha ao baixar banco limpo do S3: " + exception.getMessage(), exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
