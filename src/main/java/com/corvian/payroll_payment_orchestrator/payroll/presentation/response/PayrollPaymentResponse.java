package com.corvian.payroll_payment_orchestrator.payroll.presentation.response;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollPaymentResponse(
        UUID id,
        String employeeDocumentType,
        String employeeDocumentNumber,
        String employeeFullName,
        String bankCode,
        AccountType accountType,
        String accountNumber,
        BigDecimal amount,
        PayrollPaymentStatus status
) {}
