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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new DatabaseIdempotencyAdapter(repository);
    }

    @Test
    @DisplayName("should reject cached replay when payload hash differs")
    void testRejectCachedReplayWithDifferentPayload() {
        String key = "idem-key";
        String endpoint = "POST:/api/v1/payroll-batches";
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setRequestHash("hash-original");
        entity.setLocked(false);
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
        entity.setResponseBody(null);

        when(repository.findForUpdateByIdempotencyKeyAndEndpoint(key, endpoint)).thenReturn(Optional.of(entity));
        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        boolean locked = adapter.lock(key, endpoint, "hash-original");

        assertThat(locked).isTrue();
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().isLocked()).isTrue();
    }
}
