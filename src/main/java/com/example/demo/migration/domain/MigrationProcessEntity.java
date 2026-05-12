package com.example.demo.migration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "migration_processes")
public class MigrationProcessEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 120)
    private String clientName;

    @Column(length = 18)
    private String cnpj;

    @Column(nullable = false, length = 30)
    private String eagleVersion;

    @Column(nullable = false, length = 500)
    private String migratorDatabase;

    @Column(nullable = false, length = 500)
    private String cleanDatabasePath;

    @Column(length = 500)
    private String eagleWorkingDatabasePath;

    @Column(length = 500)
    private String finalDatabasePath;

    @Column(length = 255)
    private String finalDatabaseFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, columnDefinition = "varchar(40)")
    private MigrationStatus status = MigrationStatus.CRIADO;

    private Integer defaultDistrictId;

    @Column(length = 10)
    private String defaultCep;

    @Column(length = 2)
    private String companyState;

    private boolean migrateReceivables;

    @Column(length = 2048)
    private String lastError;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MigrationSheetEntity> sheets = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcedureExecutionEntity> procedureExecutions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEagleVersion() {
        return eagleVersion;
    }

    public void setEagleVersion(String eagleVersion) {
        this.eagleVersion = eagleVersion;
    }

    public String getMigratorDatabase() {
        return migratorDatabase;
    }

    public void setMigratorDatabase(String migratorDatabase) {
        this.migratorDatabase = migratorDatabase;
    }

    public String getCleanDatabasePath() {
        return cleanDatabasePath;
    }

    public void setCleanDatabasePath(String cleanDatabasePath) {
        this.cleanDatabasePath = cleanDatabasePath;
    }

    public String getEagleWorkingDatabasePath() {
        return eagleWorkingDatabasePath;
    }

    public void setEagleWorkingDatabasePath(String eagleWorkingDatabasePath) {
        this.eagleWorkingDatabasePath = eagleWorkingDatabasePath;
    }

    public String getFinalDatabasePath() {
        return finalDatabasePath;
    }

    public void setFinalDatabasePath(String finalDatabasePath) {
        this.finalDatabasePath = finalDatabasePath;
    }

    public String getFinalDatabaseFilename() {
        return finalDatabaseFilename;
    }

    public void setFinalDatabaseFilename(String finalDatabaseFilename) {
        this.finalDatabaseFilename = finalDatabaseFilename;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public Integer getDefaultDistrictId() {
        return defaultDistrictId;
    }

    public void setDefaultDistrictId(Integer defaultDistrictId) {
        this.defaultDistrictId = defaultDistrictId;
    }

    public String getDefaultCep() {
        return defaultCep;
    }

    public void setDefaultCep(String defaultCep) {
        this.defaultCep = defaultCep;
    }

    public String getCompanyState() {
        return companyState;
    }

    public void setCompanyState(String companyState) {
        this.companyState = companyState;
    }

    public boolean isMigrateReceivables() {
        return migrateReceivables;
    }

    public void setMigrateReceivables(boolean migrateReceivables) {
        this.migrateReceivables = migrateReceivables;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<MigrationSheetEntity> getSheets() {
        return sheets;
    }

    public List<ProcedureExecutionEntity> getProcedureExecutions() {
        return procedureExecutions;
    }
}
