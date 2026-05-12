package com.example.demo.migration.repository;

import com.example.demo.migration.domain.MigrationProcessEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationProcessRepository extends JpaRepository<MigrationProcessEntity, UUID> {
}
