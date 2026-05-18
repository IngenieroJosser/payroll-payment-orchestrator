package com.corvian.payroll_payment_orchestrator.companies.presentation;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBankAccountRequest(
        @NotBlank String bankCode,
        @NotNull AccountType accountType,
        @NotBlank String accountNumber
) {}
