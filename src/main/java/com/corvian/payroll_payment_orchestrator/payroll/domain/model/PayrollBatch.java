package com.corvian.payroll_payment_orchestrator.payroll.domain.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PayrollBatch(
        UUID id,
        UUID companyId,
        UUID sourceAccountId,
        String currency,
        LocalDate scheduledDate,
        PayrollBatchStatus status,
        BigDecimal totalAmount,
        Integer totalPayments,
        List<PayrollPayment> payments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
