package com.corvian.payroll_payment_orchestrator.iam.presentation;
import jakarta.validation.constraints.NotBlank;
public record ClientCredentialsTokenRequest(@NotBlank String grantType, @NotBlank String clientId, @NotBlank String clientSecret) {}
