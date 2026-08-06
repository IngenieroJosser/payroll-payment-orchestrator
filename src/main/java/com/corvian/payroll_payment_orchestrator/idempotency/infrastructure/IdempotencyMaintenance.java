package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class IdempotencyMaintenance {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyMaintenance.class);

    private final JpaIdempotencyKeyRepository repository;
    private final Clock clock;

    public IdempotencyMaintenance(JpaIdempotencyKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-fixed-delay-ms:3600000}")
    @Transactional
    public void deleteExpired() {
        long deleted = repository.deleteByExpiresAtBefore(OffsetDateTime.now(clock));
        if (deleted > 0) log.debug("Deleted {} expired idempotency records", deleted);
    }
}
