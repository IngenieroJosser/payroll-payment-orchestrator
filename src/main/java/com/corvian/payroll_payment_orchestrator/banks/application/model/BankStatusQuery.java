package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.util.UUID;

public record BankStatusQuery(
        UUID submissionId,
        String externalBatchId,
        BankConnectionProfile connectionProfile,
        String correlationId
) {}
