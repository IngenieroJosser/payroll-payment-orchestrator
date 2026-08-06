package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.time.OffsetDateTime;
import java.util.List;

public record BankReconciliationResult(
        String sourceReference,
        List<BankPaymentResult> payments,
        OffsetDateTime reconciledAt
) {
    public BankReconciliationResult {
        payments = payments == null ? List.of() : List.copyOf(payments);
    }
}
