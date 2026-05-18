package com.corvian.payroll_payment_orchestrator.iam.presentation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
public record CreateUserRequest(UUID tenantId, UUID companyId, @Email @NotBlank String email, @NotBlank String fullName, @NotBlank String password, List<String> roles) {}
