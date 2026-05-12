package com.example.demo.migration.service;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.controller.dto.CleanDatabaseTemplateResponse;
import com.example.demo.migration.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CleanDatabaseTemplateService {

    private final MigrationProperties properties;

    public CleanDatabaseTemplateService(MigrationProperties properties) {
        this.properties = properties;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
