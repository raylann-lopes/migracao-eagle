package com.example.demo.migration.service;

import com.example.demo.migration.config.MigrationProperties;
import com.example.demo.migration.domain.MigrationProcessEntity;
import com.example.demo.migration.exception.BusinessException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
        Path processWorkDir = Path.of(properties.getWorkDir())
                .toAbsolutePath()
                .normalize()
                .resolve(process.getId().toString());
        Path workingDatabase = processWorkDir.resolve("EAGLEERP.FDB");
        String filename = finalFilename(process);
        Path finalDatabase = processWorkDir.resolve(filename);
        String finalStorageUri = finalStorageUri(process, filename);

        try {
            Files.createDirectories(workingDatabase.getParent());
            copyCleanDatabaseTemplate(process.getCleanDatabasePath(), workingDatabase);

            // O alias EAGLEERP do Firebird aponta para a raiz do work dir.
            // As procedures alteram esse arquivo, entao ele deve ser publicado no final.
            Path rootWorkingDb = procedureDatabasePath();
            Files.copy(workingDatabase, rootWorkingDb, StandardCopyOption.REPLACE_EXISTING);

            try {
                Runtime.getRuntime().exec("chmod 666 " + rootWorkingDb.toAbsolutePath());
            } catch (Exception e) {
                // O Firebird ainda pode conseguir acessar via permissao herdada do volume.
            }

            return new PreparedDatabase(workingDatabase, finalDatabase, filename, finalStorageUri);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao preparar banco limpo do Eagle: " + exception.getMessage(), exception);
        }
    }

    public String publishFinalDatabase(PreparedDatabase preparedDatabase) {
        try {
            Files.copy(procedureDatabasePath(), preparedDatabase.finalDatabasePath(), StandardCopyOption.REPLACE_EXISTING);
            uploadToS3(preparedDatabase.finalDatabaseStorageUri(), preparedDatabase.finalDatabasePath());
            cleanupLocalDatabases(preparedDatabase);
            return preparedDatabase.finalDatabaseStorageUri();
        } catch (IOException | SdkException exception) {
            throw new BusinessException("Falha ao gerar banco final para download: " + exception.getMessage(), exception);
        }
    }

    private Path procedureDatabasePath() {
        return Path.of(properties.getWorkDir())
                .toAbsolutePath()
                .normalize()
                .resolve("EAGLEERP.FDB");
    }

    public Resource finalDatabaseResource(MigrationProcessEntity process) {
        if (process.getFinalDatabasePath() == null || process.getFinalDatabasePath().isBlank()) {
            throw new BusinessException("Banco final ainda nao foi gerado.");
        }
        if (process.getFinalDatabasePath().startsWith("s3://")) {
            return streamFromS3(process.getFinalDatabasePath());
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

        throw new BusinessException("Banco limpo deve estar configurado no S3 para execucao em nuvem.");
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
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                s3Client.getObject(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                        ResponseTransformer.toOutputStream(outputStream));
            }
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException("Banco limpo nao encontrado no S3. Bucket: " + bucket
                        + ", chave: " + key
                        + ". Configure a variavel MIGRATION_CLEAN_DB_<VERSAO>_S3_KEY com o caminho exato do arquivo.",
                        exception);
            }
            throw new BusinessException("Falha ao baixar banco limpo do S3: " + exception.getMessage(), exception);
        } catch (SdkException exception) {
            throw new BusinessException("Falha ao baixar banco limpo do S3: " + exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao preparar diretorio temporario do banco: " + exception.getMessage(), exception);
        }
    }

    private void uploadToS3(String storageUri, Path source) {
        URI uri = URI.create(storageUri);
        String bucket = uri.getHost();
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (bucket == null || bucket.isBlank() || key.isBlank()) {
            throw new BusinessException("Destino S3 do banco final invalido: " + storageUri);
        }
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/octet-stream")
                        .build(),
                RequestBody.fromFile(source));
    }

    private Resource streamFromS3(String storageUri) {
        URI uri = URI.create(storageUri);
        String bucket = uri.getHost();
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (bucket == null || bucket.isBlank() || key.isBlank()) {
            throw new BusinessException("Banco final S3 invalido: " + storageUri);
        }
        try {
            ResponseInputStream<?> inputStream = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build());
            return new InputStreamResource(inputStream);
        } catch (SdkException exception) {
            throw new BusinessException("Falha ao baixar banco final do S3: " + exception.getMessage(), exception);
        }
    }

    private void cleanupLocalDatabases(PreparedDatabase preparedDatabase) throws IOException {
        Files.deleteIfExists(preparedDatabase.finalDatabasePath());
        Files.deleteIfExists(preparedDatabase.workingDatabasePath());
        Files.deleteIfExists(procedureDatabasePath());
        deleteDirectoryIfEmpty(preparedDatabase.workingDatabasePath().getParent());
    }

    private void deleteDirectoryIfEmpty(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            if (!entries.iterator().hasNext()) {
                Files.delete(directory);
            }
        }
    }

    private String finalStorageUri(MigrationProcessEntity process, String filename) {
        String bucket = properties.getFinalDatabase().getS3().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new BusinessException("Bucket S3 do banco final nao configurado.");
        }
        String prefix = properties.getFinalDatabase().getS3().getPrefix();
        String keyPrefix = prefix == null ? "" : prefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        String key = (keyPrefix.isBlank() ? "" : keyPrefix + "/")
                + process.getEagleVersion()
                + "/"
                + process.getId()
                + "/"
                + filename;
        return "s3://" + bucket + "/" + key;
    }
}
