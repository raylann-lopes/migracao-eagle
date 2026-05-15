package com.example.demo.migration.repository;

import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.domain.MigrationSheetEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationSheetRepository extends JpaRepository<MigrationSheetEntity, Long> {

    Optional<MigrationSheetEntity> findByProcessIdAndModule(Long processId, MigrationModule module);
}
