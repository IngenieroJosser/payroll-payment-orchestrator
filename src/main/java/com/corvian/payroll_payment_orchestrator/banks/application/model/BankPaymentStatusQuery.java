package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.util.UUID;

public record BankPaymentStatusQuery(
        UUID submissionId,
        UUID paymentId,
        String externalPaymentId,
        BankConnectionProfile connectionProfile,
        String correlationId
) {}
