package com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaReconciliationItemRepository extends JpaRepository<ReconciliationItemEntity, UUID> {
    List<ReconciliationItemEntity> findByBatchIdOrderByCreatedAtDesc(UUID batchId);
    Optional<ReconciliationItemEntity> findByBatchIdAndBankReference(UUID batchId, String bankReference);
    Optional<ReconciliationItemEntity> findBySourceEventId(String sourceEventId);
}
