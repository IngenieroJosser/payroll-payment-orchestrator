package com.corvian.payroll_payment_orchestrator.idempotency.application.port;

public interface IdempotencyStorePort {
    boolean lock(String key, String endpoint, String requestHash);
    String getResponse(String key, String endpoint);
    void saveResponse(String key, String endpoint, String responseBody, int ttlHours);
    void unlock(String key, String endpoint);
}
