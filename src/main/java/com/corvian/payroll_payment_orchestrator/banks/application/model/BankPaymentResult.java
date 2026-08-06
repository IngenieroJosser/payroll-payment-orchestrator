package com.corvian.payroll_payment_orchestrator.banks.application.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BankPaymentResult(
        UUID paymentId,
        String externalPaymentId,
        String externalStatus,
        PayrollPaymentStatus status,
        String rejectionCode,
        String rejectionReason,
        OffsetDateTime settledAt
) {}
