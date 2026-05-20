package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import com.corvian.payroll_payment_orchestrator.idempotency.application.port.IdempotencyStorePort;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseIdempotencyAdapter implements IdempotencyStorePort {
    private final JpaIdempotencyKeyRepository repository;

    public DatabaseIdempotencyAdapter(JpaIdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean lock(String key, String endpoint, String requestHash) {
        Optional<IdempotencyKeyEntity> existing = repository.findByIdempotencyKeyAndEndpoint(key, endpoint);
        if (existing.isPresent()) {
            IdempotencyKeyEntity entity = existing.get();
            if (!entity.getRequestHash().equals(requestHash)) {
                throw new DomainException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was reused with a different request");
            }
            return false; // key is either locked or completed
        }
        
        try {
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
            entity.setId(UUID.randomUUID());
            entity.setIdempotencyKey(key);
            entity.setEndpoint(endpoint);
            entity.setRequestHash(requestHash);
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
            entity.setLocked(true);
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getResponse(String key, String endpoint) {
        return repository.findByIdempotencyKeyAndEndpoint(key, endpoint)
                .map(IdempotencyKeyEntity::getResponseBody)
                .orElse(null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResponse(String key, String endpoint, String responseBody, int ttlHours) {
        repository.findByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(entity -> {
            entity.setResponseBody(responseBody);
            entity.setLocked(false);
            entity.setExpiresAt(OffsetDateTime.now().plusHours(ttlHours));
            repository.saveAndFlush(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlock(String key, String endpoint) {
        repository.findByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(entity -> {
            if (entity.isLocked() && entity.getResponseBody() == null) {
                repository.delete(entity);
            }
        });
    }
}
