package com.corvian.payroll_payment_orchestrator.banks.application.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record BankPaymentInstruction(
        UUID paymentId,
        String beneficiaryName,
        String documentType,
        String documentNumber,
        String destinationBankCode,
        AccountType accountType,
        String accountNumber,
        BigDecimal amount,
        String paymentReference
) {}
