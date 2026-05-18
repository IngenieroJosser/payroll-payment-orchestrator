package com.corvian.payroll_payment_orchestrator.idempotency.application;

import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyKeyEntity;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.JpaIdempotencyKeyRepository;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final JpaIdempotencyKeyRepository repository;
    private final CryptoService cryptoService;

    public IdempotencyService(JpaIdempotencyKeyRepository repository, CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public void registerOrReject(String key, String endpoint, String requestFingerprint) {
        String requestHash = cryptoService.hmacSha256(requestFingerprint);
        repository.findByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new DomainException("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was reused with a different request");
            }
            throw new DomainException("DUPLICATE_IDEMPOTENT_REQUEST", "This request was already received and protected against duplicate execution");
        });
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(key);
        entity.setEndpoint(endpoint);
        entity.setRequestHash(requestHash);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        repository.save(entity);
    }
}
