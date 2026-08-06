package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.time.LocalDate;
import java.util.UUID;

public record BankReconciliationCommand(
        UUID tenantId,
        UUID companyId,
        UUID submissionId,
        LocalDate fromDate,
        LocalDate toDate,
        BankConnectionProfile connectionProfile,
        String correlationId
) {}
