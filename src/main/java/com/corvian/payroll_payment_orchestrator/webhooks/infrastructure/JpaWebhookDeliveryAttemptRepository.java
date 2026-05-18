package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
public interface JpaWebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttemptEntity, UUID> {
    List<WebhookDeliveryAttemptEntity> findTop50ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(String status, OffsetDateTime nextRetryAt);
}
