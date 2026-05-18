package com.corvian.payroll_payment_orchestrator.payroll.application.command;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;

import java.math.BigDecimal;

public record CreatePayrollPaymentCommand(
        String employeeDocumentType,
        String employeeDocumentNumber,
        String employeeFullName,
        String bankCode,
        AccountType accountType,
        String accountNumber,
        BigDecimal amount
) {}
