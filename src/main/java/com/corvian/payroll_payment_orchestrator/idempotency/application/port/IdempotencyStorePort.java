package com.corvian.payroll_payment_orchestrator.idempotency.application.port;

public interface IdempotencyStorePort {
    boolean lock(String key, String endpoint, String requestHash);
    String getResponse(String key, String endpoint, String requestHash);
    void saveResponse(String key, String endpoint, String responseBody, int ttlHours);
    void unlock(String key, String endpoint);

    default StoredIdempotencyResponse getStoredResponse(String key, String endpoint, String requestHash) {
        String body = getResponse(key, endpoint, requestHash);
        return body == null ? null : new StoredIdempotencyResponse(200, "application/json", body);
    }

    default void saveResponse(String key, String endpoint, int status, String contentType, String responseBody, int ttlHours) {
        saveResponse(key, endpoint, responseBody, ttlHours);
    }
}
