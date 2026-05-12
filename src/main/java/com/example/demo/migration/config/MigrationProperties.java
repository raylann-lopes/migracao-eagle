package com.example.demo.migration.config;

import com.example.demo.migration.domain.MigrationModule;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.migration")
public class MigrationProperties {

    private Firebird firebird = new Firebird();
    private CleanDatabase cleanDatabase = new CleanDatabase();
    private FinalDatabase finalDatabase = new FinalDatabase();
    private String workDir = "/tmp/migracao-eagle";

    public Firebird getFirebird() {
        return firebird;
    }

    public void setFirebird(Firebird firebird) {
        this.firebird = firebird;
    }

    public CleanDatabase getCleanDatabase() {
        return cleanDatabase;
    }

    public void setCleanDatabase(CleanDatabase cleanDatabase) {
        this.cleanDatabase = cleanDatabase;
    }

    public FinalDatabase getFinalDatabase() {
        return finalDatabase;
    }

    public void setFinalDatabase(FinalDatabase finalDatabase) {
        this.finalDatabase = finalDatabase;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public static class Firebird {
        private boolean enabled;
        private String jdbcUrl;
        private String username;
        private String password;
        private String migratorDatabase = "./data/MIGRADOR.FDB";
        private String eagleAliasName = "EAGLEERP";
        private Map<MigrationModule, String> tableMap = new EnumMap<>(MigrationModule.class);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMigratorDatabase() {
            return migratorDatabase;
        }

        public void setMigratorDatabase(String migratorDatabase) {
            this.migratorDatabase = migratorDatabase;
        }

        public String getEagleAliasName() {
            return eagleAliasName;
        }

        public void setEagleAliasName(String eagleAliasName) {
            this.eagleAliasName = eagleAliasName;
        }

        public Map<MigrationModule, String> getTableMap() {
            return tableMap;
        }

        public void setTableMap(Map<MigrationModule, String> tableMap) {
            this.tableMap = tableMap;
        }
    }

    public static class CleanDatabase {
        private S3 s3 = new S3();
        private List<Template> templates = new ArrayList<>();
        private String cacheDir = "/tmp/migracao-eagle/templates";

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }

        public List<Template> getTemplates() {
            return templates;
        }

        public void setTemplates(List<Template> templates) {
            this.templates = templates;
        }

        public String getCacheDir() {
            return cacheDir;
        }

        public void setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
        }
    }

    public static class S3 {
        private String bucket = "eagle-migracao-templates";
        private String region = "sa-east-1";
        private String prefix = "";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public static class FinalDatabase {
        private S3 s3 = new S3();
        private String downloadCacheDir = "/tmp/migracao-eagle/downloads";

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }

        public String getDownloadCacheDir() {
            return downloadCacheDir;
        }

        public void setDownloadCacheDir(String downloadCacheDir) {
            this.downloadCacheDir = downloadCacheDir;
        }
    }

    public static class Template {
        private String version;
        private String description;
        private String s3Key;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getS3Key() {
            return s3Key;
        }

        public void setS3Key(String s3Key) {
            this.s3Key = s3Key;
        }

    }
}
