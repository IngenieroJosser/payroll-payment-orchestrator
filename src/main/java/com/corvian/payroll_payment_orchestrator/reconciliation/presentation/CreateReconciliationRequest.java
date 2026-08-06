package com.corvian.payroll_payment_orchestrator.reconciliation.presentation;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateReconciliationRequest(
        @NotBlank @Size(max=120) String bankReference,
        @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal bankAmount,
        @Size(max=180) String sourceEventId,
        @Size(max=500) String details
) {}
