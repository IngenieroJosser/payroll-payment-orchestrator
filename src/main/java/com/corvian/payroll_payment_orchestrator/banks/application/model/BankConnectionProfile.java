package com.corvian.payroll_payment_orchestrator.banks.application.model;

import java.net.URI;
import java.util.UUID;

public record BankConnectionProfile(
        UUID id,
        UUID tenantId,
        UUID companyId,
        String bankCode,
        String providerKey,
        String environment,
        URI baseUri,
        String apiToken,
        int connectTimeoutMs,
        int readTimeoutMs
) {}
