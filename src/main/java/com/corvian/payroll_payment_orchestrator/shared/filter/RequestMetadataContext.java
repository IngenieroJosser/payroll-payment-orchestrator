package com.corvian.payroll_payment_orchestrator.shared.filter;

import org.springframework.stereotype.Component;

@Component
public class RequestMetadataContext {
    private final ThreadLocal<RequestMetadata> current = new ThreadLocal<>();

    public void set(String correlationId, String clientIp) {
        current.set(new RequestMetadata(correlationId, clientIp));
    }

    public RequestMetadata get() {
        RequestMetadata metadata = current.get();
        return metadata == null ? new RequestMetadata(null, null) : metadata;
    }

    public void clear() {
        current.remove();
    }

    public record RequestMetadata(String correlationId, String clientIp) {}
}
