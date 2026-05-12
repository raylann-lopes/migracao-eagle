package com.example.demo.migration.repository;

import com.example.demo.migration.domain.MigrationModule;
import com.example.demo.migration.domain.MigrationSheetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationSheetRepository extends JpaRepository<MigrationSheetEntity, UUID> {

    Optional<MigrationSheetEntity> findByProcessIdAndModule(UUID processId, MigrationModule module);

    List<MigrationSheetEntity> findByProcessId(UUID processId);

    void deleteByProcessIdAndModule(UUID processId, MigrationModule module);
}
