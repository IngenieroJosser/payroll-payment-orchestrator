package com.corvian.payroll_payment_orchestrator.idempotency.application;

import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyKeyEntity;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.JpaIdempotencyKeyRepository;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * IdempotencyService provides idempotence guarantees for API operations.
 *
 * Behavior:
 * - First request with a new idempotency key: Register and allow execution
 * - Duplicate request (same key + same payload): Return cached response or mark as replayable
 * - Duplicate request (same key + different payload): Reject with IDEMPOTENCY_KEY_REUSED error
 * - Request during processing (locked): Reject with informative error
 *
 * Limitations & Future Work (Phase 2):
 * - Single instance: No distributed locking yet (no Redis)
 * - Database-level concurrency control via pessimistic locking
 * - Response cache limited to 24 hours by default
 * - No async notification on completion
 */
@Service
public class IdempotencyService {
    private final JpaIdempotencyKeyRepository repository;
    private final CryptoService cryptoService;

    public IdempotencyService(JpaIdempotencyKeyRepository repository, CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    /**
     * Legacy method: Register or reject idempotency key.
     * Maintained for backward compatibility.
     * Throws DomainException if key was already used.
     */
    @Transactional
    public void registerOrReject(String key, String endpoint, String requestFingerprint) {
        String requestHash = cryptoService.hmacSha256(requestFingerprint);
        repository.findByIdempotencyKeyAndEndpoint(key, endpoint).ifPresent(existing -> {
            if (!Objects.equals(existing.getRequestHash(), requestHash)) {
                throw new DomainException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was reused with a different request");
            }
            throw new DomainException("DUPLICATE_IDEMPOTENT_REQUEST",
                "This request was already received and protected against duplicate execution");
        });
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(key);
        entity.setEndpoint(endpoint);
        entity.setRequestHash(requestHash);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        entity.setLocked(false);
        repository.save(entity);
    }

    /**
     * Register a new idempotency key with request hash.
     * Marks it as "in progress" (locked=true) until completion is signaled.
     *
     * @param key Unique idempotency key (UUID or custom format)
     * @param endpoint API endpoint path
     * @param requestFingerprint Request payload fingerprint or serialized request
     * @return UUID of the idempotency record for later reference
     * @throws DomainException if key was already used with different payload
     */
    @Transactional
    public UUID registerRequest(String key, String endpoint, String requestFingerprint) {
        String requestHash = cryptoService.hmacSha256(requestFingerprint);
        Optional<IdempotencyKeyEntity> existing = repository.findByIdempotencyKeyAndEndpoint(key, endpoint);

        if (existing.isPresent()) {
            IdempotencyKeyEntity entity = existing.get();
            if (!Objects.equals(entity.getRequestHash(), requestHash)) {
                throw new DomainException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was reused with a different request payload");
            }
            // Same key + same payload: if already completed, can replay
            if (!entity.isLocked() && entity.getResponseBody() != null) {
                return entity.getId();
            }
            // If still locked, operation is in progress
            if (entity.isLocked()) {
                throw new DomainException("OPERATION_IN_PROGRESS",
                    "An operation with this idempotency key is already in progress");
            }
            entity.setLocked(true);
            repository.save(entity);
            return entity.getId();
        }

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(key);
        entity.setEndpoint(endpoint);
        entity.setRequestHash(requestHash);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        entity.setLocked(true); // Mark as in-progress
        entity.setResponseBody(null);
        repository.save(entity);
        return entity.getId();
    }

    /**
     * Check if idempotency key exists with a stored response.
     * Used to replay responses for duplicate requests.
     *
     * @param key Idempotency key
     * @param endpoint Endpoint path
     * @return Optional containing the stored response body if request was previously completed
     */
    @Transactional(readOnly = true)
    public Optional<String> getStoredResponse(String key, String endpoint) {
        return repository.findByIdempotencyKeyAndEndpoint(key, endpoint)
            .filter(entity -> !entity.isLocked())
            .map(IdempotencyKeyEntity::getResponseBody)
            .filter(response -> response != null && !response.isEmpty());
    }

    /**
     * Mark an idempotency key as completed with its response.
     * Unlocks the key so subsequent identical requests can be replayed.
     *
     * @param idempotencyId UUID of the idempotency record (from registerRequest)
     * @param responseBody JSON/serialized response to cache
     */
    @Transactional
    public void markComplete(UUID idempotencyId, String responseBody) {
        repository.findById(idempotencyId).ifPresent(entity -> {
            entity.setResponseBody(responseBody);
            entity.setLocked(false);
            repository.save(entity);
        });
    }

    /**
     * Mark an idempotency key as failed (operation did not complete).
     * Unlocks the key so a retry is possible (new request, not replay).
     *
     * @param idempotencyId UUID of the idempotency record
     */
    @Transactional
    public void markFailed(UUID idempotencyId) {
        repository.findById(idempotencyId).ifPresent(entity -> {
            entity.setLocked(false);
            entity.setResponseBody(null);
            repository.save(entity);
        });
    }

    /**
     * Check if an operation is currently locked (in progress).
     *
     * @param key Idempotency key
     * @param endpoint Endpoint path
     * @return true if operation is in progress, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isLocked(String key, String endpoint) {
        return repository.findByIdempotencyKeyAndEndpoint(key, endpoint)
            .map(IdempotencyKeyEntity::isLocked)
            .orElse(false);
    }

    /**
     * Validate request hash matches stored hash.
     * Used for safety checks in concurrent scenarios.
     *
     * @param key Idempotency key
     * @param endpoint Endpoint path
     * @param requestFingerprint Current request fingerprint
     * @return true if hash matches, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validatePayload(String key, String endpoint, String requestFingerprint) {
        String requestHash = cryptoService.hmacSha256(requestFingerprint);
        return repository.findByIdempotencyKeyAndEndpoint(key, endpoint)
            .map(entity -> Objects.equals(entity.getRequestHash(), requestHash))
            .orElse(false);
    }
}
