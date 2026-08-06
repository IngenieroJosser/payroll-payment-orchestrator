package com.corvian.payroll_payment_orchestrator.payroll.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePayrollBatchRequest(
        @NotNull UUID companyId,
        @NotNull UUID sourceAccountId,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate scheduledDate,
        @Valid @NotEmpty @Size(max = 10000) List<CreatePayrollPaymentRequest> payments
) {}
