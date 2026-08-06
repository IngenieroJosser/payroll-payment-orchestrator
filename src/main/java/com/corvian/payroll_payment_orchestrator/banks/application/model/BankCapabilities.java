package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.util.Set;

public record BankCapabilities(
        boolean batchSubmission,
        boolean batchStatus,
        boolean paymentStatus,
        boolean reconciliation,
        boolean idempotency,
        int maxPaymentsPerBatch,
        Set<String> supportedCurrencies
) {
    public BankCapabilities {
        supportedCurrencies = supportedCurrencies == null ? Set.of() : Set.copyOf(supportedCurrencies);
    }
}
