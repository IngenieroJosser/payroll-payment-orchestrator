package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlPolicy;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookService {
    private final JpaWebhookEndpointRepository repository;
    private final AuditLogService auditLogService;
    private final ResourceAccessService accessService;
    private final OutboundUrlPolicy outboundUrlPolicy;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebhookService(JpaWebhookEndpointRepository repository, AuditLogService auditLogService,
                          ResourceAccessService accessService, OutboundUrlPolicy outboundUrlPolicy, Clock clock) {
        this.repository = repository; this.auditLogService = auditLogService; this.accessService = accessService;
        this.outboundUrlPolicy = outboundUrlPolicy; this.clock = clock;
    }

    @Transactional
    public CreatedWebhookEndpoint createWithSecret(UUID companyId, String url) {
        var company = accessService.requireCompanyAccess(companyId);
        String validatedUrl = outboundUrlPolicy.validate(url).toString();
        String secret = generateSecret();
        OffsetDateTime now = OffsetDateTime.now(clock);
        WebhookEndpointEntity entity = new WebhookEndpointEntity();
        entity.setId(UUID.randomUUID()); entity.setTenantId(company.getTenantId()); entity.setCompanyId(companyId);
        entity.setUrl(validatedUrl); entity.setSecret("ENCRYPTED"); entity.setSecretCiphertext(secret);
        entity.setEnabled(true); entity.setCreatedAt(now); entity.setUpdatedAt(now);
        WebhookEndpointEntity saved = repository.save(entity);
        auditLogService.record("WEBHOOK_ENDPOINT_CREATED", "WEBHOOK_ENDPOINT", saved.getId(),
                "Webhook endpoint registered", company.getTenantId(), companyId);
        return new CreatedWebhookEndpoint(saved, secret);
    }

    @Transactional(readOnly=true)
    public List<WebhookEndpointEntity> findByCompanyId(UUID companyId) {
        var company = accessService.requireCompanyAccess(companyId);
        return repository.findByTenantIdAndCompanyId(company.getTenantId(), companyId);
    }

    @Transactional
    public void disable(UUID companyId, UUID webhookId) {
        var company = accessService.requireCompanyAccess(companyId);
        WebhookEndpointEntity entity = repository.findByIdAndCompanyId(webhookId, companyId)
                .orElseThrow(() -> new DomainException("WEBHOOK_ENDPOINT_NOT_FOUND", "Webhook endpoint was not found"));
        entity.setEnabled(false); entity.setUpdatedAt(OffsetDateTime.now(clock));
        auditLogService.record("WEBHOOK_ENDPOINT_DISABLED", "WEBHOOK_ENDPOINT", webhookId,
                "Webhook endpoint disabled", company.getTenantId(), companyId);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
