package com.corvian.payroll_payment_orchestrator.payroll.presentation.request;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record CreatePayrollPaymentRequest(
        @NotBlank @Size(max = 20) String employeeDocumentType,
        @NotBlank @Size(max = 80) String employeeDocumentNumber,
        @NotBlank @Size(max = 180) String employeeFullName,
        @NotBlank @Size(max = 20) String bankCode,
        @NotNull AccountType accountType,
        @NotBlank @Size(min = 4, max = 34) String accountNumber,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount
) {}
