package com.example.demo.migration.config;

import com.example.demo.migration.domain.MigrationModule;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app.migration")
public class MigrationProperties {

    private Firebird firebird = new Firebird();
    private CleanDatabase cleanDatabase = new CleanDatabase();
    private FinalDatabase finalDatabase = new FinalDatabase();
    private String workDir = "/tmp/migracao-eagle";

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Firebird {
        private boolean enabled;
        private String jdbcUrl;
        private String username;
        private String password;
        private String migratorDatabase = "./data/MIGRADOR.FDB";
        private String eagleAliasName = "EAGLEERP";
        private Map<MigrationModule, String> tableMap = new EnumMap<>(MigrationModule.class);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanDatabase {
        private S3 s3 = new S3();
        private List<Template> templates = new ArrayList<>();
        private String cacheDir = "/tmp/migracao-eagle/templates";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3 {
        private String bucket = "eagle-migracao-templates";
        private String region = "sa-east-1";
        private String prefix = "";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalDatabase {
        private S3 s3 = new S3();
        private String downloadCacheDir = "/tmp/migracao-eagle/downloads";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Template {
        private String version;
        private String description;
        private String s3Key;
    }
}
