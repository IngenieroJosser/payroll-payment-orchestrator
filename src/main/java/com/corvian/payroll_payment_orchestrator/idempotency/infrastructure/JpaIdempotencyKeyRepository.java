package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface JpaIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    Optional<IdempotencyKeyEntity> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);
}
