package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaWebhookEndpointRepository extends JpaRepository<WebhookEndpointEntity, UUID> {
    List<WebhookEndpointEntity> findByCompanyId(UUID companyId);
    List<WebhookEndpointEntity> findByCompanyIdAndEnabledTrue(UUID companyId);
}
