package com.example.demo.migration.repository;

import com.example.demo.migration.domain.MigrationProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationProcessRepository extends JpaRepository<MigrationProcessEntity, Long> {
}
