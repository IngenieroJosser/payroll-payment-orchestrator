package com.corvian.payroll_payment_orchestrator.banks.application;

import java.math.BigDecimal;
import java.util.UUID;

public record BankBatchRequest(
        UUID batchId,
        UUID companyId,
        BigDecimal totalAmount,
        Integer totalPayments,
        String currency
) {}
