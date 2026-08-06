package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankPaymentProvider;
import com.corvian.payroll_payment_orchestrator.banks.application.model.*;
import com.corvian.payroll_payment_orchestrator.banks.domain.exception.BankProviderException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generic reference adapter. It is intentionally not branded as a certified bank integration.
 * A real bank adapter must translate its proprietary contract behind BankPaymentProvider and pass
 * the shared provider contract tests before being enabled.
 */
@Component
public class RestBankPaymentProvider implements BankPaymentProvider {
    private final ObjectMapper objectMapper;

    public RestBankPaymentProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override public String providerKey() { return "REST_GENERIC"; }

    @Override
    public BankSubmissionResult submitPayrollBatch(BankSubmissionCommand command) {
        try {
            var profile = command.connectionProfile();
            byte[] body = objectMapper.writeValueAsBytes(new SubmissionPayload(
                    command.payrollBatchId(), command.executionId(), command.bankIdempotencyKey(), command.currency(),
                    command.executionDate(), command.sourceAccountNumber(), command.payments()));
            JsonNode response = invoke(profile, "POST", "/payroll-batches", body,
                    command.bankIdempotencyKey(), command.correlationId());
            return new BankSubmissionResult(requiredText(response, "externalBatchId"),
                    mapStatus(text(response, "status")), text(response, "status"), text(response, "message"),
                    OffsetDateTime.now(ZoneOffset.UTC));
        } catch (BankProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BankProviderException("BANK_SUBMISSION_SERIALIZATION_FAILED", "Unable to construct or parse bank submission", false);
        }
    }

    @Override
    public BankPaymentStatusResult getBatchStatus(BankStatusQuery query) {
        JsonNode response = invoke(query.connectionProfile(), "GET",
                "/payroll-batches/" + encodePath(query.externalBatchId()), null, null, query.correlationId());
        String externalBatchId = text(response, "externalBatchId");
        return new BankPaymentStatusResult(externalBatchId == null ? query.externalBatchId() : externalBatchId,
                mapStatus(text(response, "status")), text(response, "status"), parsePayments(response.path("payments")),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankPaymentStatusResult getPaymentStatus(BankPaymentStatusQuery query) {
        JsonNode response = invoke(query.connectionProfile(), "GET",
                "/payments/" + encodePath(query.externalPaymentId()), null, null, query.correlationId());
        return new BankPaymentStatusResult(null, mapStatus(text(response, "status")), text(response, "status"),
                parsePayments(response.isArray() ? response : objectMapper.createArrayNode().add(response)),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankReconciliationResult reconcile(BankReconciliationCommand command) {
        String path = "/reconciliation?from=" + command.fromDate() + "&to=" + command.toDate();
        JsonNode response = invoke(command.connectionProfile(), "GET", path, null, null, command.correlationId());
        return new BankReconciliationResult(text(response, "sourceReference"), parsePayments(response.path("payments")),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankCapabilities getCapabilities() {
        return new BankCapabilities(true, true, true, true, true, 10_000, Set.of());
    }

    private JsonNode invoke(BankConnectionProfile profile, String method, String path, byte[] body,
                            String idempotencyKey, String correlationId) {
        try {
            URI uri = appendPath(profile.baseUri(), path);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(profile.connectTimeoutMs()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(profile.readTimeoutMs()))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Payroll-Payment-Orchestrator/1.0");
            if (profile.apiToken() != null && !profile.apiToken().isBlank()) {
                builder.header("Authorization", "Bearer " + profile.apiToken());
            }
            if (correlationId != null && !correlationId.isBlank()) builder.header("X-Correlation-ID", correlationId);
            if (idempotencyKey != null && !idempotencyKey.isBlank()) builder.header("Idempotency-Key", idempotencyKey);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofByteArray(body));

            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                boolean retryable = status == 408 || status == 425 || status == 429 || status >= 500;
                throw new BankProviderException("BANK_PROVIDER_HTTP_" + status,
                        "Bank provider returned HTTP status " + status, retryable);
            }
            if (response.body() == null || response.body().length == 0) return objectMapper.createObjectNode();
            return objectMapper.readTree(response.body());
        } catch (BankProviderException ex) {
            throw ex;
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new BankProviderException("BANK_PROVIDER_TIMEOUT", "Bank provider timed out", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BankProviderException("BANK_PROVIDER_INTERRUPTED", "Bank provider call was interrupted", true);
        } catch (Exception ex) {
            throw new BankProviderException("BANK_PROVIDER_UNAVAILABLE", "Bank provider is unavailable", true);
        }
    }

    private URI appendPath(URI base, String path) {
        String root = base.toString().replaceAll("/+$", "");
        return URI.create(root + (path.startsWith("/") ? path : "/" + path));
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new BankProviderException("BANK_PROVIDER_INVALID_RESPONSE", "Bank response did not include " + field, false);
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String encodePath(String value) {
        if (value == null || !value.matches("^[A-Za-z0-9_.:-]{1,180}$")) {
            throw new BankProviderException("BANK_PROVIDER_INVALID_REFERENCE", "Bank reference contains invalid characters", false);
        }
        return value;
    }

    private BankSubmissionStatus mapStatus(String value) {
        if (value == null) return BankSubmissionStatus.UNKNOWN;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "RECEIVED", "ACCEPTED", "SENT_TO_BANK" -> BankSubmissionStatus.ACCEPTED;
            case "IN_PROCESS", "PROCESSING", "PENDING" -> BankSubmissionStatus.PROCESSING;
            case "PARTIAL", "PARTIALLY_SETTLED", "PARTIALLY_PAID" -> BankSubmissionStatus.PARTIALLY_SETTLED;
            case "PAID", "SETTLED", "COMPLETED", "SUCCESS" -> BankSubmissionStatus.SETTLED;
            case "REJECTED", "DECLINED" -> BankSubmissionStatus.REJECTED;
            case "FAILED", "ERROR", "CANCELLED" -> BankSubmissionStatus.FAILED;
            default -> BankSubmissionStatus.UNKNOWN;
        };
    }

    private List<BankPaymentResult> parsePayments(JsonNode paymentsNode) {
        if (paymentsNode == null || !paymentsNode.isArray()) return List.of();
        java.util.ArrayList<BankPaymentResult> results = new java.util.ArrayList<>();
        for (JsonNode node : paymentsNode) {
            String paymentId = text(node, "paymentId");
            if (paymentId == null) continue;
            BankSubmissionStatus normalized = mapStatus(text(node, "status"));
            PayrollPaymentStatus paymentStatus = switch (normalized) {
                case SETTLED -> PayrollPaymentStatus.PAID;
                case REJECTED -> PayrollPaymentStatus.REJECTED;
                case FAILED -> PayrollPaymentStatus.FAILED;
                case ACCEPTED -> PayrollPaymentStatus.SENT_TO_BANK;
                case PROCESSING, PARTIALLY_SETTLED, PREPARED, SUBMITTING, UNKNOWN -> PayrollPaymentStatus.PROCESSING;
            };
            try {
                results.add(new BankPaymentResult(java.util.UUID.fromString(paymentId), text(node, "externalPaymentId"),
                        text(node, "status"), paymentStatus, text(node, "rejectionCode"), text(node, "rejectionReason"), null));
            } catch (IllegalArgumentException ex) {
                throw new BankProviderException("BANK_PROVIDER_INVALID_RESPONSE",
                        "Bank response contains an invalid paymentId", false);
            }
        }
        return List.copyOf(results);
    }

    private record SubmissionPayload(
            java.util.UUID batchId,
            java.util.UUID executionId,
            String idempotencyKey,
            String currency,
            java.time.LocalDate executionDate,
            String sourceAccountNumber,
            List<BankPaymentInstruction> payments
    ) {}
}
