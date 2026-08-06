package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaWebhookEndpointRepository extends JpaRepository<WebhookEndpointEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select endpoint from WebhookEndpointEntity endpoint where endpoint.id = :id")
    Optional<WebhookEndpointEntity> findByIdForUpdate(@Param("id") UUID id);
    List<WebhookEndpointEntity> findByCompanyId(UUID companyId);
    List<WebhookEndpointEntity> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
    List<WebhookEndpointEntity> findByCompanyIdAndEnabledTrue(UUID companyId);
    Optional<WebhookEndpointEntity> findByIdAndCompanyId(UUID id, UUID companyId);
}
