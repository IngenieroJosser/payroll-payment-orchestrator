package com.corvian.payroll_payment_orchestrator.companies.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCompanyRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 180) String legalName,
        @NotBlank @Size(max = 40) String taxId,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
