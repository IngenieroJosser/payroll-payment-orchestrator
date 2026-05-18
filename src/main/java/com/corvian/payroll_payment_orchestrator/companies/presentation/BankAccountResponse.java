package com.corvian.payroll_payment_orchestrator.companies.presentation;

import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        UUID companyId,
        String bankCode,
        AccountType accountType,
        String accountNumberMasked,
        String accountNumberLast4,
        BankAccountStatus status,
        OffsetDateTime createdAt
) {}
