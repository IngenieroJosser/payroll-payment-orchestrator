package com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBankSubmissionRepository extends JpaRepository<BankSubmissionEntity, UUID> {
    Optional<BankSubmissionEntity> findFirstByBatchIdOrderByCreatedAtDesc(UUID batchId);
    Optional<BankSubmissionEntity> findByExecutionId(UUID executionId);
    @Query(value="""
        SELECT * FROM bank_submissions
        WHERE status IN ('ACCEPTED','PROCESSING','PARTIALLY_SETTLED')
          AND next_status_poll_at IS NOT NULL AND next_status_poll_at <= :now
        ORDER BY next_status_poll_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery=true)
    List<BankSubmissionEntity> lockDueForPolling(@Param("now") OffsetDateTime now, @Param("limit") int limit);
    @Query(value="""
        SELECT id FROM bank_submissions
        WHERE status IN ('ACCEPTED','PROCESSING','PARTIALLY_SETTLED')
          AND next_status_poll_at IS NOT NULL AND next_status_poll_at <= :now
        ORDER BY next_status_poll_at
        LIMIT :limit
        """, nativeQuery=true)
    List<UUID> findDueIds(@Param("now") OffsetDateTime now, @Param("limit") int limit);
    long countByStatus(com.corvian.payroll_payment_orchestrator.banks.application.model.BankSubmissionStatus status);

}
