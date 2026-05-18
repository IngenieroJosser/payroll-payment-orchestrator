package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.JpaWebhookEndpointRepository;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.WebhookEndpointEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookService {
    private final JpaWebhookEndpointRepository repository;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebhookService(JpaWebhookEndpointRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public WebhookEndpointEntity create(UUID companyId, String url) {
        WebhookEndpointEntity entity = new WebhookEndpointEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setUrl(url.trim());
        entity.setSecret(generateSecret());
        entity.setEnabled(true);
        entity.setCreatedAt(OffsetDateTime.now());
        WebhookEndpointEntity saved = repository.save(entity);
        auditLogService.record("WEBHOOK_ENDPOINT_CREATED", "WEBHOOK_ENDPOINT", saved.getId(), "Webhook endpoint registered for company " + companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointEntity> findByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId);
    }

    @Transactional
    public void disable(UUID webhookId) {
        WebhookEndpointEntity entity = repository.findById(webhookId)
                .orElseThrow(() -> new DomainException("WEBHOOK_ENDPOINT_NOT_FOUND", "Webhook endpoint was not found"));
        entity.setEnabled(false);
        repository.save(entity);
        auditLogService.record("WEBHOOK_ENDPOINT_DISABLED", "WEBHOOK_ENDPOINT", webhookId, "Webhook endpoint disabled");
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
