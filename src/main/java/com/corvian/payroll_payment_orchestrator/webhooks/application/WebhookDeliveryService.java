package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {
    private final JpaWebhookEndpointRepository endpointRepository;
    private final JpaWebhookDeliveryAttemptRepository attemptRepository;
    private final WebhookSigner signer;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public WebhookDeliveryService(JpaWebhookEndpointRepository endpointRepository, JpaWebhookDeliveryAttemptRepository attemptRepository, WebhookSigner signer, ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.attemptRepository = attemptRepository;
        this.signer = signer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(UUID companyId, String event, UUID resourceId, Object data) {
        endpointRepository.findByCompanyIdAndEnabledTrue(companyId).forEach(endpoint -> deliver(endpoint, event, resourceId, data, 1));
    }

    private void deliver(WebhookEndpointEntity endpoint, String event, UUID resourceId, Object data, int attemptNumber) {
        WebhookDeliveryAttemptEntity attempt = new WebhookDeliveryAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setWebhookEndpointId(endpoint.getId());
        attempt.setEvent(event);
        attempt.setResourceId(resourceId);
        attempt.setAttempt(attemptNumber);
        attempt.setCreatedAt(OffsetDateTime.now());
        try {
            String payload = objectMapper.writeValueAsString(Map.of("event", event, "resourceId", resourceId, "data", data, "occurredAt", OffsetDateTime.now().toString()));
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String signature = signer.sign(endpoint.getSecret(), timestamp, payload);
            Integer status = restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Timestamp", timestamp)
                    .header("X-Webhook-Signature", signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
            attempt.setHttpStatus(status);
            attempt.setStatus(status >= 200 && status < 300 ? "DELIVERED" : "RETRY_PENDING");
            if (status < 200 || status >= 300) attempt.setNextRetryAt(nextRetry(attemptNumber));
        } catch (Exception ex) {
            attempt.setStatus("RETRY_PENDING");
            attempt.setErrorMessage(ex.getMessage() == null ? "Webhook delivery failed" : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())));
            attempt.setNextRetryAt(nextRetry(attemptNumber));
        }
        attemptRepository.save(attempt);
    }

    @Scheduled(fixedDelayString = "${app.webhooks.retry-fixed-delay-ms:60000}")
    @Transactional
    public void retryPending() {
        attemptRepository.findTop50ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc("RETRY_PENDING", OffsetDateTime.now()).forEach(previous -> {
            endpointRepository.findById(previous.getWebhookEndpointId()).filter(WebhookEndpointEntity::getEnabled).ifPresent(endpoint -> {
                if (previous.getAttempt() < 5) {
                    deliver(endpoint, previous.getEvent(), previous.getResourceId(), Map.of("retry", true), previous.getAttempt() + 1);
                    previous.setStatus("RETRIED");
                } else {
                    previous.setStatus("FAILED");
                }
                attemptRepository.save(previous);
            });
        });
    }

    private OffsetDateTime nextRetry(int attempt) {
        long seconds = switch (attempt) {
            case 1 -> 60;
            case 2 -> 300;
            case 3 -> 900;
            default -> 1800;
        };
        return OffsetDateTime.now().plusSeconds(seconds);
    }
}
