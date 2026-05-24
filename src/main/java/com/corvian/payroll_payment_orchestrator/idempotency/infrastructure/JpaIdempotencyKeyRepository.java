package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface JpaIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    Optional<IdempotencyKeyEntity> findByIdempotencyKeyAndEndpoint(String idempotencyKey, String endpoint);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select entity
            from IdempotencyKeyEntity entity
            where entity.idempotencyKey = :idempotencyKey
              and entity.endpoint = :endpoint
            """)
    Optional<IdempotencyKeyEntity> findForUpdateByIdempotencyKeyAndEndpoint(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("endpoint") String endpoint
    );
}
