package com.example.demo.migration.repository;

import com.example.demo.migration.domain.ProcedureExecutionEntity;
import java.util.Optional;
import com.example.demo.migration.domain.ProcedureExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecutionEntity, Long> {

    Optional<ProcedureExecutionEntity> findFirstByProcessIdAndStatusOrderByStepOrder(
            Long processId,
            ProcedureExecutionStatus status);
}
