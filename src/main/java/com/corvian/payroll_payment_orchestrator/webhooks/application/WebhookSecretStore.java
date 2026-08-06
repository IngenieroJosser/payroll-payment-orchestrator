package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.JpaWebhookEndpointRepository;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.WebhookEndpointEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WebhookSecretStore {
    private final JpaWebhookEndpointRepository repository;
    private final Clock clock;

    public WebhookSecretStore(JpaWebhookEndpointRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public String getSigningSecret(UUID endpointId) {
        WebhookEndpointEntity endpoint = repository.findByIdForUpdate(endpointId)
                .orElseThrow(() -> new DomainException("WEBHOOK_ENDPOINT_NOT_FOUND", "Webhook endpoint was not found"));
        if (endpoint.getSecretCiphertext() != null && !endpoint.getSecretCiphertext().isBlank()) {
            return endpoint.getSecretCiphertext();
        }
        String legacySecret = endpoint.getSecret();
        if (legacySecret == null || legacySecret.isBlank() || "ENCRYPTED".equals(legacySecret)) {
            throw new DomainException("WEBHOOK_SECRET_UNAVAILABLE", "Webhook signing secret is unavailable");
        }
        endpoint.setSecretCiphertext(legacySecret);
        endpoint.setSecret("ENCRYPTED");
        endpoint.setUpdatedAt(OffsetDateTime.now(clock));
        return legacySecret;
    }
}
