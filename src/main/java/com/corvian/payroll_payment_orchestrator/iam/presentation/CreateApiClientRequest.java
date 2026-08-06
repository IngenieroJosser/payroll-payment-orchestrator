package com.corvian.payroll_payment_orchestrator.iam.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateApiClientRequest(
        @NotNull UUID companyId,
        @NotBlank String name,
        List<String> scopes
) {}
