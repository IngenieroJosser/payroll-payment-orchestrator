package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.time.OffsetDateTime;

public record BankSubmissionResult(
        String externalBatchId,
        BankSubmissionStatus status,
        String providerStatus,
        String message,
        OffsetDateTime receivedAt
) {}
