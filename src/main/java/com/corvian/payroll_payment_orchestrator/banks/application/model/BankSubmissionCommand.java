package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BankSubmissionCommand(
        UUID tenantId,
        UUID companyId,
        UUID payrollBatchId,
        UUID executionId,
        String bankIdempotencyKey,
        String currency,
        LocalDate executionDate,
        String sourceAccountNumber,
        List<BankPaymentInstruction> payments,
        BankConnectionProfile connectionProfile,
        String correlationId
) {
    public BankSubmissionCommand {
        payments = List.copyOf(payments);
    }
}
