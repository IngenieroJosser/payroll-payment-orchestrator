package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.time.OffsetDateTime;
import java.util.List;

public record BankPaymentStatusResult(
        String externalBatchId,
        BankSubmissionStatus status,
        String providerStatus,
        List<BankPaymentResult> payments,
        OffsetDateTime checkedAt
) {
    public BankPaymentStatusResult {
        payments = payments == null ? List.of() : List.copyOf(payments);
    }
}
