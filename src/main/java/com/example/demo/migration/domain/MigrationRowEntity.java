package com.example.demo.migration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "migration_rows")
public class MigrationRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_id", nullable = false)
    private MigrationSheetEntity sheet;

    @Column(nullable = false)
    private int rowNumber;

    @Column(nullable = false)
    private boolean valid;

    @Column(nullable = false, columnDefinition = "text")
    private String normalizedJson;

    @Column(columnDefinition = "text")
    private String errorsJson;

    @Column(columnDefinition = "text")
    private String warningsJson;
}
