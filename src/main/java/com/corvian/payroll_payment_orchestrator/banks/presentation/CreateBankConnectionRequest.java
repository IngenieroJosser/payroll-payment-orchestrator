package com.corvian.payroll_payment_orchestrator.banks.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBankConnectionRequest(
        @NotNull UUID companyId,
        @NotBlank @Size(max = 40) String bankCode,
        @Size(max = 60) String providerKey,
        @Size(max = 30) String environment,
        @Size(max = 500) String baseUrl,
        @Size(max = 1000) String apiToken,
        @Size(max = 255) String credentialReference,
        @Min(100) @Max(60000) Integer connectTimeoutMs,
        @Min(100) @Max(300000) Integer readTimeoutMs
) {}
