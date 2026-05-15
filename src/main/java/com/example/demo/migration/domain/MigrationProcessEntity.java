package com.example.demo.migration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "migration_processes")
public class MigrationProcessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
