package com.corvian.payroll_payment_orchestrator.iam.presentation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
