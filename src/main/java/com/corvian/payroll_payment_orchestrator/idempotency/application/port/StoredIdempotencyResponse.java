package com.corvian.payroll_payment_orchestrator.idempotency.application.port;
public record StoredIdempotencyResponse(int status, String contentType, String body) {}
