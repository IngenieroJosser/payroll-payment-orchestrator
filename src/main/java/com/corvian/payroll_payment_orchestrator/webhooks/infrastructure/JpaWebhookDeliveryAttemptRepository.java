package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaWebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttemptEntity, UUID> {
    @Query(value="""
        SELECT id FROM webhook_delivery_attempts
        WHERE (
                status IN ('PENDING','RETRY_PENDING')
                AND (next_retry_at IS NULL OR next_retry_at <= :now)
              )
           OR (status = 'SENDING' AND updated_at <= :staleBefore)
        ORDER BY created_at LIMIT :limit
        """, nativeQuery=true)
    List<UUID> findDueIds(@Param("now") OffsetDateTime now,
                          @Param("staleBefore") OffsetDateTime staleBefore,
                          @Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WebhookDeliveryAttemptEntity w where w.id=:id")
    Optional<WebhookDeliveryAttemptEntity> findByIdForUpdate(@Param("id") UUID id);
    long countByStatus(String status);
}
