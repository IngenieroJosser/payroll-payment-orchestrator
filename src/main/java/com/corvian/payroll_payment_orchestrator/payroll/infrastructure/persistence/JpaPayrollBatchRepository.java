package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPayrollBatchRepository extends JpaRepository<PayrollBatchEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = "payments")
    Optional<PayrollBatchEntity> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "payments")
    @Query("select batch from PayrollBatchEntity batch where batch.id = :id")
    Optional<PayrollBatchEntity> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "payments")
    List<PayrollBatchEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "payments")
    List<PayrollBatchEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @EntityGraph(attributePaths = "payments")
    List<PayrollBatchEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
