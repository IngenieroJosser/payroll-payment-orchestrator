package com.corvian.payroll_payment_orchestrator.iam.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotNull UUID tenantId,
        UUID companyId,
        @Email @NotBlank String email,
        @NotBlank @Size(max = 180) String fullName,
        @NotBlank @Size(min = 12, max = 128) String password,
        List<String> roles
) {}
