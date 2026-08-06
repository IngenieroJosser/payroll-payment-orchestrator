package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import com.corvian.payroll_payment_orchestrator.idempotency.application.port.IdempotencyStorePort;
import com.corvian.payroll_payment_orchestrator.idempotency.application.port.StoredIdempotencyResponse;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseIdempotencyAdapter implements IdempotencyStorePort {
    private final JpaIdempotencyKeyRepository repository;
    private final Clock clock;
    private final long lockLeaseSeconds;

    public DatabaseIdempotencyAdapter(
            JpaIdempotencyKeyRepository repository,
            Clock clock,
            @Value("${app.idempotency.lock-lease-seconds:300}") long lockLeaseSeconds
    ) {
        this.repository = repository;
        this.clock = clock;
        this.lockLeaseSeconds = Math.max(30, lockLeaseSeconds);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean lock(String key, String endpoint, String requestHash) {
        Optional<IdempotencyKeyEntity> existing = repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (existing.isPresent()) {
            IdempotencyKeyEntity entity = existing.get();
            if (entity.getExpiresAt().isBefore(now)) {
                repository.delete(entity);
                repository.flush();
            } else {
                validateHash(entity, requestHash);
                if (hasStoredResponse(entity)) return false;
                if (entity.isLocked() && !lockLeaseExpired(entity, now)) return false;
                entity.setLocked(true);
                entity.setLockedAt(now);
                entity.setUpdatedAt(now);
                repository.saveAndFlush(entity);
                return true;
            }
        }
        try {
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
            entity.setId(UUID.randomUUID()); entity.setIdempotencyKey(key); entity.setEndpoint(endpoint);
            entity.setRequestHash(requestHash); entity.setCreatedAt(now); entity.setUpdatedAt(now);
            entity.setExpiresAt(now.plusHours(24)); entity.setLocked(true); entity.setLockedAt(now);
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException ex) { return false; }
    }

    @Override
    @Transactional(readOnly = true)
    public String getResponse(String key, String endpoint, String requestHash) {
        StoredIdempotencyResponse stored = getStoredResponse(key, endpoint, requestHash);
        return stored == null ? null : stored.body();
    }

    @Override
    @Transactional(readOnly = true)
    public StoredIdempotencyResponse getStoredResponse(String key, String endpoint, String requestHash) {
        return repository.findByIdempotencyKeyAndEndpoint(key, endpoint).map(entity -> {
            validateHash(entity, requestHash);
            if (entity.isLocked() || !hasStoredResponse(entity) || entity.getExpiresAt().isBefore(OffsetDateTime.now(clock))) return null;
            return new StoredIdempotencyResponse(entity.getResponseStatus() == null ? 200 : entity.getResponseStatus(),
                    entity.getResponseContentType() == null ? "application/json" : entity.getResponseContentType(), entity.getResponseBody());
        }).orElse(null);
    }

    @Override
    public void saveResponse(String key, String endpoint, String responseBody, int ttlHours) {
        saveResponse(key, endpoint, 200, "application/json", responseBody, ttlHours);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResponse(String key, String endpoint, int status, String contentType, String responseBody, int ttlHours) {
        repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(entity -> {
            OffsetDateTime now = OffsetDateTime.now(clock);
            entity.setResponseBody(responseBody); entity.setResponseStatus(status); entity.setResponseContentType(contentType);
            entity.setLocked(false); entity.setLockedAt(null); entity.setExpiresAt(now.plusHours(ttlHours)); entity.setUpdatedAt(now);
            repository.saveAndFlush(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlock(String key, String endpoint) {
        repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(entity -> {
            if (entity.isLocked() && entity.getResponseBody() == null) repository.delete(entity);
        });
    }

    private boolean lockLeaseExpired(IdempotencyKeyEntity entity, OffsetDateTime now) {
        OffsetDateTime lockedAt = entity.getLockedAt() == null ? entity.getUpdatedAt() : entity.getLockedAt();
        return lockedAt == null || !lockedAt.isAfter(now.minusSeconds(lockLeaseSeconds));
    }

    private void validateHash(IdempotencyKeyEntity entity, String requestHash) {
        if (!Objects.equals(entity.getRequestHash(), requestHash)) {
            throw new DomainException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was reused with a different request");
        }
    }
    private boolean hasStoredResponse(IdempotencyKeyEntity entity) { return entity.getResponseBody() != null; }
}
