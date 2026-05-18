package com.corvian.payroll_payment_orchestrator.reconciliation.presentation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record CreateReconciliationRequest(@NotBlank String bankReference, @NotNull BigDecimal bankAmount) {}
