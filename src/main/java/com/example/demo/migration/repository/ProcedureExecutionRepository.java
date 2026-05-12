package com.example.demo.migration.repository;

import com.example.demo.migration.domain.ProcedureExecutionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecutionEntity, UUID> {

    List<ProcedureExecutionEntity> findByProcessIdOrderByStepOrder(UUID processId);

    Optional<ProcedureExecutionEntity> findFirstByProcessIdAndStatusOrderByStepOrder(
            UUID processId,
            com.example.demo.migration.domain.ProcedureExecutionStatus status);
}
