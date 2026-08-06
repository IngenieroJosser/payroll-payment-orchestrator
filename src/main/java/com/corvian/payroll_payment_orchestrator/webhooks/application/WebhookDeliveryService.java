package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlPolicy;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private final JpaWebhookEndpointRepository endpointRepository;
    private final JpaWebhookDeliveryAttemptRepository attemptRepository;
    private final WebhookAttemptStore attemptStore;
    private final WebhookSigner signer;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;
    private final WebhookSecretStore secretStore;
    private final OutboundUrlPolicy outboundUrlPolicy;
    private final Clock clock;
    private final int maxAttempts;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final long sendingLeaseMs;
    private final HttpClient httpClient;

    public WebhookDeliveryService(
            JpaWebhookEndpointRepository endpointRepository,
            JpaWebhookDeliveryAttemptRepository attemptRepository,
            WebhookAttemptStore attemptStore,
            WebhookSigner signer,
            ObjectMapper objectMapper,
            CryptoService cryptoService,
            WebhookSecretStore secretStore,
            OutboundUrlPolicy outboundUrlPolicy,
            Clock clock,
            @Value("${app.webhooks.max-attempts:5}") int maxAttempts,
            @Value("${app.webhooks.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.webhooks.read-timeout-ms:15000}") int readTimeoutMs,
            @Value("${app.webhooks.sending-lease-ms:120000}") long sendingLeaseMs
    ) {
        this.endpointRepository = endpointRepository; this.attemptRepository = attemptRepository;
        this.attemptStore = attemptStore; this.signer = signer; this.objectMapper = objectMapper;
        this.cryptoService = cryptoService; this.secretStore = secretStore; this.outboundUrlPolicy = outboundUrlPolicy; this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.connectTimeoutMs = Math.max(100, connectTimeoutMs);
        this.readTimeoutMs = Math.max(100, readTimeoutMs);
        this.sendingLeaseMs = Math.max(this.readTimeoutMs + 5_000L, sendingLeaseMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Transactional
    public void publish(UUID companyId, String event, UUID resourceId, Object data) {
        try {
            UUID eventId = UUID.randomUUID();
            OffsetDateTime occurredAt = OffsetDateTime.now(clock);
            String payload = objectMapper.writeValueAsString(Map.of(
                    "id", eventId, "event", event, "resourceId", resourceId,
                    "occurredAt", occurredAt, "data", data));
            String payloadHash = cryptoService.hmacSha256(payload);
            for (WebhookEndpointEntity endpoint : endpointRepository.findByCompanyIdAndEnabledTrue(companyId)) {
                WebhookDeliveryAttemptEntity attempt = new WebhookDeliveryAttemptEntity();
                attempt.setId(UUID.randomUUID()); attempt.setWebhookEndpointId(endpoint.getId()); attempt.setEventId(eventId);
                attempt.setEvent(event); attempt.setResourceId(resourceId); attempt.setAttempt(1); attempt.setStatus("PENDING");
                attempt.setPayload(payload); attempt.setPayloadHash(payloadHash); attempt.setCreatedAt(occurredAt);
                attempt.setUpdatedAt(occurredAt); attemptRepository.save(attempt);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist webhook event", ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.webhooks.retry-fixed-delay-ms:60000}")
    public void deliverPending() {
        for (UUID id : attemptStore.dueIds(50, sendingLeaseMs)) {
            WebhookDeliveryAttemptEntity attempt = attemptStore.claim(id, sendingLeaseMs);
            if (attempt == null) continue;
            WebhookEndpointEntity endpoint = endpointRepository.findById(attempt.getWebhookEndpointId()).orElse(null);
            if (endpoint != null && Boolean.TRUE.equals(endpoint.getEnabled())) {
                deliver(endpoint, attempt);
            } else {
                attemptStore.failed(id, null, "Webhook endpoint is disabled or missing", maxAttempts);
            }
        }
    }

    private void deliver(WebhookEndpointEntity endpoint, WebhookDeliveryAttemptEntity attempt) {
        try {
            URI uri = outboundUrlPolicy.validate(endpoint.getUrl());
            String timestamp = String.valueOf(Instant.now(clock).getEpochSecond());
            String secret = secretStore.getSigningSecret(endpoint.getId());
            String signature = signer.sign(secret, timestamp, attempt.getPayload());
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(readTimeoutMs))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Payroll-Payment-Orchestrator-Webhook/1.0")
                    .header("X-Webhook-Id", attempt.getEventId().toString())
                    .header("X-Webhook-Event", attempt.getEvent())
                    .header("X-Webhook-Timestamp", timestamp)
                    .header("X-Webhook-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(attempt.getPayload(), StandardCharsets.UTF_8)).build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 200 && status < 300) attemptStore.delivered(attempt.getId(), status);
            else attemptStore.failed(attempt.getId(), status, "Webhook endpoint returned HTTP " + status, maxAttempts);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            attemptStore.failed(attempt.getId(), null, "Webhook delivery interrupted", maxAttempts);
        } catch (Exception ex) {
            log.warn("Webhook delivery failed. eventId={}, endpointId={}", attempt.getEventId(), endpoint.getId());
            attemptStore.failed(attempt.getId(), null, ex.getMessage(), maxAttempts);
        }
    }
}
