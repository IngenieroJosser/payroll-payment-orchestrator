package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.idempotency.application.IdempotencyService;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyKeyEntity;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.JpaIdempotencyKeyRepository;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IdempotencyService Tests")
class IdempotencyServiceTest {

    @Mock
    private JpaIdempotencyKeyRepository repository;

    @Mock
    private CryptoService cryptoService;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new IdempotencyService(repository, cryptoService);
    }

    @Test
    @DisplayName("should register new idempotency key")
    void testRegisterNewKey() {
        // Arrange
        String key = "idem-key-123";
        String endpoint = "/api/v1/payroll-batches";
        String requestFingerprint = "{\"amount\": 1000}";
        String requestHash = "hash-xyz";

        when(cryptoService.hmacSha256(requestFingerprint)).thenReturn(requestHash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.empty());

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        // Act
        UUID idempotencyId = service.registerRequest(key, endpoint, requestFingerprint);

        // Assert
        assertThat(idempotencyId).isNotNull();
        verify(repository).save(captor.capture());
        IdempotencyKeyEntity saved = captor.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo(key);
        assertThat(saved.getEndpoint()).isEqualTo(endpoint);
        assertThat(saved.getRequestHash()).isEqualTo(requestHash);
        assertThat(saved.isLocked()).isTrue(); // Should be marked as in-progress
        assertThat(saved.getResponseBody()).isNull();
    }

    @Test
    @DisplayName("should reject duplicate key with different payload")
    void testRejectDuplicateKeyDifferentPayload() {
        // Arrange
        String key = "idem-key-456";
        String endpoint = "/api/v1/payroll-batches";
        String newPayload = "{\"amount\": 2000}";
        String oldHash = "hash-old";
        String newHash = "hash-new";

        IdempotencyKeyEntity existingEntity = new IdempotencyKeyEntity();
        existingEntity.setIdempotencyKey(key);
        existingEntity.setRequestHash(oldHash);

        when(cryptoService.hmacSha256(newPayload)).thenReturn(newHash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(existingEntity));

        // Act & Assert
        assertThatThrownBy(() -> service.registerRequest(key, endpoint, newPayload))
            .isInstanceOf(DomainException.class)
            .isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    @DisplayName("should reject duplicate key with same payload if still locked")
    void testRejectDuplicateKeyStillLocked() {
        // Arrange
        String key = "idem-key-789";
        String endpoint = "/api/v1/payroll-batches";
        String payload = "{\"amount\": 1500}";
        String hash = "hash-consistent";

        IdempotencyKeyEntity existingEntity = new IdempotencyKeyEntity();
        existingEntity.setIdempotencyKey(key);
        existingEntity.setRequestHash(hash);
        existingEntity.setLocked(true); // Still in progress

        when(cryptoService.hmacSha256(payload)).thenReturn(hash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(existingEntity));

        // Act & Assert
        assertThatThrownBy(() -> service.registerRequest(key, endpoint, payload))
            .isInstanceOf(DomainException.class)
            .isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("OPERATION_IN_PROGRESS"));
    }

    @Test
    @DisplayName("should return stored response for duplicate completed request")
    void testReturnStoredResponseForDuplicate() {
        // Arrange
        String key = "idem-key-duplicate";
        String endpoint = "/api/v1/payroll-batches";
        String payload = "{\"amount\": 2500}";
        String hash = "hash-consistent";
        String responseBody = "{\"id\": \"batch-123\", \"status\": \"CREATED\"}";

        IdempotencyKeyEntity existingEntity = new IdempotencyKeyEntity();
        existingEntity.setId(UUID.randomUUID());
        existingEntity.setIdempotencyKey(key);
        existingEntity.setRequestHash(hash);
        existingEntity.setLocked(false);
        existingEntity.setResponseBody(responseBody);

        when(cryptoService.hmacSha256(payload)).thenReturn(hash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(existingEntity));

        // Act
        UUID idempotencyId = service.registerRequest(key, endpoint, payload);

        // Assert
        assertThat(idempotencyId).isEqualTo(existingEntity.getId());
    }

    @Test
    @DisplayName("should relock same key and payload after failed operation")
    void testRelockFailedOperationForRetry() {
        // Arrange
        String key = "idem-key-retry";
        String endpoint = "/api/v1/payroll-batches";
        String payload = "{\"amount\": 2600}";
        String hash = "hash-retry";
        UUID existingId = UUID.randomUUID();

        IdempotencyKeyEntity existingEntity = new IdempotencyKeyEntity();
        existingEntity.setId(existingId);
        existingEntity.setIdempotencyKey(key);
        existingEntity.setRequestHash(hash);
        existingEntity.setLocked(false);
        existingEntity.setResponseBody(null);

        when(cryptoService.hmacSha256(payload)).thenReturn(hash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(existingEntity));

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        // Act
        UUID idempotencyId = service.registerRequest(key, endpoint, payload);

        // Assert
        assertThat(idempotencyId).isEqualTo(existingId);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isTrue();
    }

    @Test
    @DisplayName("should mark operation as complete with response")
    void testMarkComplete() {
        // Arrange
        UUID idempotencyId = UUID.randomUUID();
        String responseBody = "{\"id\": \"batch-456\"}";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(idempotencyId);
        entity.setLocked(true);

        when(repository.findById(idempotencyId)).thenReturn(Optional.of(entity));

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        // Act
        service.markComplete(idempotencyId, responseBody);

        // Assert
        verify(repository).save(captor.capture());
        IdempotencyKeyEntity saved = captor.getValue();
        assertThat(saved.getResponseBody()).isEqualTo(responseBody);
        assertThat(saved.isLocked()).isFalse(); // Unlocked after completion
    }

    @Test
    @DisplayName("should mark operation as failed and allow retry")
    void testMarkFailed() {
        // Arrange
        UUID idempotencyId = UUID.randomUUID();

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(idempotencyId);
        entity.setLocked(true);
        entity.setResponseBody("{\"error\": \"something\"}");

        when(repository.findById(idempotencyId)).thenReturn(Optional.of(entity));

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        // Act
        service.markFailed(idempotencyId);

        // Assert
        verify(repository).save(captor.capture());
        IdempotencyKeyEntity saved = captor.getValue();
        assertThat(saved.isLocked()).isFalse();
        assertThat(saved.getResponseBody()).isNull(); // Clear response to allow retry
    }

    @Test
    @DisplayName("should retrieve stored response for completed request")
    void testGetStoredResponse() {
        // Arrange
        String key = "idem-key-get";
        String endpoint = "/api/v1/payroll-batches";
        String responseBody = "{\"id\": \"batch-789\"}";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setEndpoint(endpoint);
        entity.setLocked(false);
        entity.setResponseBody(responseBody);

        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        // Act
        Optional<String> result = service.getStoredResponse(key, endpoint);

        // Assert
        assertThat(result).hasValue(responseBody);
    }

    @Test
    @DisplayName("should not return stored response if operation is locked")
    void testGetStoredResponseWhileLocked() {
        // Arrange
        String key = "idem-key-locked";
        String endpoint = "/api/v1/payroll-batches";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setLocked(true); // Still in progress

        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        // Act
        Optional<String> result = service.getStoredResponse(key, endpoint);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should check if operation is locked")
    void testIsLocked() {
        // Arrange
        String key = "idem-key-check";
        String endpoint = "/api/v1/payroll-batches";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setLocked(true);

        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        // Act
        boolean isLocked = service.isLocked(key, endpoint);

        // Assert
        assertThat(isLocked).isTrue();
    }

    @Test
    @DisplayName("should return false for non-existent idempotency key when checking lock")
    void testIsLockedNonExistent() {
        // Arrange
        String key = "idem-key-nonexist";
        String endpoint = "/api/v1/payroll-batches";

        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.empty());

        // Act
        boolean isLocked = service.isLocked(key, endpoint);

        // Assert
        assertThat(isLocked).isFalse();
    }

    @Test
    @DisplayName("should validate payload matches stored hash")
    void testValidatePayloadMatches() {
        // Arrange
        String key = "idem-key-validate";
        String endpoint = "/api/v1/payroll-batches";
        String payload = "{\"amount\": 3000}";
        String hash = "hash-valid";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setRequestHash(hash);

        when(cryptoService.hmacSha256(payload)).thenReturn(hash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        // Act
        boolean isValid = service.validatePayload(key, endpoint, payload);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("should detect payload mismatch")
    void testValidatePayloadMismatch() {
        // Arrange
        String key = "idem-key-mismatch";
        String endpoint = "/api/v1/payroll-batches";
        String payload = "{\"amount\": 3000}";
        String storedHash = "hash-old";
        String newHash = "hash-new";

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setRequestHash(storedHash);

        when(cryptoService.hmacSha256(payload)).thenReturn(newHash);
        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        // Act
        boolean isValid = service.validatePayload(key, endpoint, payload);

        // Assert
        assertThat(isValid).isFalse();
    }
}

