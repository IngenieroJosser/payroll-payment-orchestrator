package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookAttemptStore {
    private final JpaWebhookDeliveryAttemptRepository repository;
    private final Clock clock;

    public WebhookAttemptStore(JpaWebhookDeliveryAttemptRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }

    @Transactional(readOnly=true)
    public List<UUID> dueIds(int limit, long sendingLeaseMs) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return repository.findDueIds(now, now.minusNanos(Math.max(1_000, sendingLeaseMs) * 1_000_000), limit);
    }

    @Transactional
    public WebhookDeliveryAttemptEntity claim(UUID id, long sendingLeaseMs) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime staleBefore = now.minusNanos(Math.max(1_000, sendingLeaseMs) * 1_000_000);
        return repository.findByIdForUpdate(id)
                .filter(entity -> isClaimable(entity, staleBefore))
                .map(entity -> {
                    entity.setStatus("SENDING");
                    entity.setUpdatedAt(now);
                    return entity;
                })
                .orElse(null);
    }

    private boolean isClaimable(WebhookDeliveryAttemptEntity entity, OffsetDateTime staleBefore) {
        if ("PENDING".equals(entity.getStatus()) || "RETRY_PENDING".equals(entity.getStatus())) return true;
        return "SENDING".equals(entity.getStatus())
                && entity.getUpdatedAt() != null
                && !entity.getUpdatedAt().isAfter(staleBefore);
    }

    @Transactional
    public void delivered(UUID id, int httpStatus) {
        repository.findByIdForUpdate(id).ifPresent(entity -> {
            OffsetDateTime now = OffsetDateTime.now(clock);
            entity.setStatus("DELIVERED"); entity.setHttpStatus(httpStatus); entity.setErrorMessage(null);
            entity.setNextRetryAt(null); entity.setCompletedAt(now); entity.setUpdatedAt(now);
        });
    }

    @Transactional
    public void failed(UUID id, Integer httpStatus, String error, int maxAttempts) {
        repository.findByIdForUpdate(id).ifPresent(entity -> {
            OffsetDateTime now = OffsetDateTime.now(clock);
            int nextAttempt = entity.getAttempt() + 1;
            entity.setAttempt(nextAttempt); entity.setHttpStatus(httpStatus); entity.setErrorMessage(sanitize(error));
            entity.setUpdatedAt(now);
            if (nextAttempt > maxAttempts) {
                entity.setStatus("FAILED"); entity.setNextRetryAt(null); entity.setCompletedAt(now);
            } else {
                entity.setStatus("RETRY_PENDING"); entity.setNextRetryAt(now.plusSeconds(backoffSeconds(nextAttempt)));
            }
        });
    }

    private long backoffSeconds(int attempt) {
        return switch (attempt) { case 1 -> 60; case 2 -> 300; case 3 -> 900; default -> 1800; };
    }
    private String sanitize(String value) {
        if (value == null) return "Webhook delivery failed";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
