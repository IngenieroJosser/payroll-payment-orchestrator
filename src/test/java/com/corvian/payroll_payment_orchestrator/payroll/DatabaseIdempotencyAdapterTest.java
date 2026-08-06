package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.DatabaseIdempotencyAdapter;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyKeyEntity;
import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.JpaIdempotencyKeyRepository;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseIdempotencyAdapter Tests")
class DatabaseIdempotencyAdapterTest {

    @Mock
    private JpaIdempotencyKeyRepository repository;

    private DatabaseIdempotencyAdapter adapter;
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-06T18:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new DatabaseIdempotencyAdapter(repository, clock, 300);
    }

    @Test
    @DisplayName("should reject cached replay when payload hash differs")
    void testRejectCachedReplayWithDifferentPayload() {
        String key = "idem-key";
        String endpoint = "POST:/api/v1/payroll-batches";
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setRequestHash("hash-original");
        entity.setLocked(false);
        entity.setExpiresAt(OffsetDateTime.now(clock).plusHours(1));
        entity.setResponseBody("{\"ok\":true}");

        when(repository.findByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> adapter.getResponse(key, endpoint, "hash-different"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Idempotency-Key was reused");
    }

    @Test
    @DisplayName("should relock unlocked key without stored response")
    void testRelockUnlockedKeyWithoutStoredResponse() {
        String key = "idem-key";
        String endpoint = "POST:/api/v1/payroll-batches";
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setRequestHash("hash-original");
        entity.setLocked(false);
        entity.setExpiresAt(OffsetDateTime.now(clock).plusHours(1));
        entity.setUpdatedAt(OffsetDateTime.now(clock));
        entity.setResponseBody(null);

        when(repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));
        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        boolean locked = adapter.lock(key, endpoint, "hash-original");

        assertThat(locked).isTrue();
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isLocked()).isTrue();
    }

    @Test
    @DisplayName("should recover an abandoned idempotency lock after the lease")
    void shouldRecoverAbandonedLock() {
        String key = "idem-stale-lock";
        String endpoint = "POST:/api/v1/payroll-batches";
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setRequestHash("hash-original");
        entity.setLocked(true);
        entity.setLockedAt(OffsetDateTime.now(clock).minusMinutes(10));
        entity.setUpdatedAt(OffsetDateTime.now(clock).minusMinutes(10));
        entity.setExpiresAt(OffsetDateTime.now(clock).plusHours(1));

        when(repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));

        assertThat(adapter.lock(key, endpoint, "hash-original")).isTrue();
        assertThat(entity.isLocked()).isTrue();
        assertThat(entity.getLockedAt()).isEqualTo(OffsetDateTime.now(clock));
        verify(repository).saveAndFlush(entity);
    }
}
