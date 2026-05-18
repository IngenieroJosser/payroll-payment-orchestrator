package com.corvian.payroll_payment_orchestrator.payroll.presentation.request;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePayrollPaymentRequest(
        @NotBlank String employeeDocumentType,
        @NotBlank String employeeDocumentNumber,
        @NotBlank String employeeFullName,
        @NotBlank String bankCode,
        @NotNull AccountType accountType,
        @NotBlank String accountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
