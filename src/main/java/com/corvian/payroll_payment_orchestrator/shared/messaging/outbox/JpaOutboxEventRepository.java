package com.corvian.payroll_payment_orchestrator.shared.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status IN ('PENDING','RETRY') AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> lockNextBatch(@Param("now") OffsetDateTime now, @Param("limit") int limit);
    long countByStatus(OutboxStatus status);

}
